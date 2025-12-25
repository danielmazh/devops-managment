import groovy.json.JsonSlurper

// Helper function that communicates with the Docker Hub API and returns a list of tag names for a repository.
def fetchDockerHubTags(String repoName, String dUser, String dPass) {
    try {
        // 1. Login to get JWT Token
        def authUrl = new URL("https://hub.docker.com/v2/users/login/")
        def authConn = authUrl.openConnection()
        authConn.setRequestMethod("POST")
        authConn.setRequestProperty("Content-Type", "application/json")
        authConn.doOutput = true

        def authPayload = "{\"username\": \"${dUser}\", \"password\": \"${dPass}\"}"
        def os = authConn.outputStream
        os.write(authPayload.getBytes("UTF-8"))
        os.close()

        if (authConn.responseCode != 200) {
            println "Login failed: ${authConn.responseCode}"
            return ["ERROR_LOGIN_FAILED"]
        }

        def authResponse = authConn.inputStream.text
        def token = new JsonSlurper().parseText(authResponse).token

        // 2. Fetch Tags
        def tagsUrl = new URL("https://hub.docker.com/v2/repositories/${dUser}/${repoName}/tags/?page_size=100")
        def tagsConn = tagsUrl.openConnection()
        tagsConn.setRequestMethod("GET")
        tagsConn.setRequestProperty("Authorization", "JWT ${token}")

        if (tagsConn.responseCode != 200) {
            println "Fetch tags failed: ${tagsConn.responseCode}"
            try { println "Error details: " + tagsConn.errorStream?.text } catch (ignored) {}
            return ["ERROR_FETCH_FAILED: ${tagsConn.responseCode}"]
        }

        def tagsResponse = tagsConn.inputStream.text
        def json = new JsonSlurper().parseText(tagsResponse)
        return json.results.collect { it.name }

    } catch (Exception e) {
        println "Exception fetching tags: ${e.message}"
        return ["ERROR: ${e.toString()}"]
    }
}

// Helper that retrieves Docker Hub tags using Jenkins credentials or environment variables as fallback.
def getTags(String repoName) {
    try {
        // If the pipeline runtime provides the `withCredentials` step, use it.
        return withCredentials([usernamePassword(credentialsId: 'docker-hub-token', usernameVariable: 'dUser', passwordVariable: 'dPass')]) {
            return fetchDockerHubTags(repoName, dUser, dPass)
        }
    } catch (MissingMethodException | NoSuchMethodError e) {
        // `withCredentials` may not be available at parse time; try environment fallback.
        def envUser = System.getenv('DOCKERHUB_USER') ?: ''
        def envPass = System.getenv('DOCKERHUB_PASS') ?: ''
        return fetchDockerHubTags(repoName, envUser, envPass)
    }
}

// Helper that runs an Ansible playbook on a list of IPs
def runAnsibleOnIps(String ipsJson, String playbookPath) {
    def ips = new groovy.json.JsonSlurper().parseText(ipsJson)
    ips.each { ip ->
        echo "Processing ${ip}..."
        // Wait for SSH port to be open
        timeout(time: 5, unit: 'MINUTES') {
            waitUntil {
                def r = sh script: "nc -z -w 5 ${ip} 22", returnStatus: true
                return (r == 0)
            }
        }
        
        echo "Running Ansible on ${ip}..."
        withCredentials([sshUserPrivateKey(credentialsId: 'daniel-devops', keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER')]) {
            sh """
                export ANSIBLE_HOST_KEY_CHECKING=False
                ansible-playbook -i '${ip},' -u ubuntu --private-key \$SSH_KEY ${playbookPath}
            """
        }
    }
}

pipeline {
    agent {label "jenkins-slave-team-3-new"}

    parameters {
        choice(name: 'BACKEND_VERSION', choices: getTags("dms-be"), description: 'Select a tag from Docker Hub')
        choice(name: 'FRONTEND_VERSION', choices: getTags("dms-fe"), description: 'Select a tag from Docker Hub')
        string(name: 'CUSTOMER_NAME', defaultValue: 'test_customer', description: 'Enter customer name')
        choice(name: 'FRONTEND_COUNT', choices: ['1','2','3'], description: 'Number of frontend instances')
        choice(name: 'BACKEND_COUNT', choices: ['1'], description: 'Number of backend instances')
        string(name: 'EMAIL_RECIPIENT', defaultValue: 'your-email@example.com', description: 'Email for notifications')
        booleanParam(name: 'SKIP_TERRAFORM', defaultValue: false, description: 'Skip Terraform Apply (Use existing infrastructure)')
    }

    stages {
        stage('Check Params') {
            steps {
                echo "Selected Backend Version is: ${params.BACKEND_VERSION}"
                echo "Selected Frontend Version is: ${params.FRONTEND_VERSION}"
                echo "Name: ${params.CUSTOMER_NAME}"
                echo "Frontend Instances: ${params.FRONTEND_COUNT}"
                echo "Backend Instances: ${params.BACKEND_COUNT}"
                echo "Skip Terraform: ${params.SKIP_TERRAFORM}"
            }
        }

        stage('Checkout Code') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'git-new-new', usernameVariable: 'gUser', passwordVariable: 'gPass')])
                {
                    sh '''
                        if [ -d devops-managment ]; then
                            rm -rf devops-managment
                        fi
                        git clone https://${gUser}:${gPass}@github.com/danielmazh/devops-managment.git
                    '''
                }
            }
        }

        stage('Trigger iac') {
            steps {
                script {
                    if (!params.SKIP_TERRAFORM) {
                        // 1. Init and Plan
                        sh """
                            pwd
                            cd devops-managment/projects/domain-monitoring-system/terraform
                            pwd
                            ls -la
                            
                            terraform init -input=false
                            
                            terraform plan \
                            -var="customer_name=${params.CUSTOMER_NAME}" \
                            -var="backend_instance_count=${params.BACKEND_COUNT}" \
                            -var="frontend_instance_count=${params.FRONTEND_COUNT}"
                        """

                        // 2. Pause for User Validation
                        input message: 'Review the plan above. Approve Apply?', ok: 'Deploy'

                        // 3. Apply (Keep -auto-approve so the command doesn't hang)
                        sh """
                            cd devops-managment/projects/domain-monitoring-system/terraform
                            
                            terraform apply \
                            -var="customer_name=${params.CUSTOMER_NAME}" \
                            -var="backend_instance_count=${params.BACKEND_COUNT}" \
                            -var="frontend_instance_count=${params.FRONTEND_COUNT}" \
                            -auto-approve
                        """
                    } else {
                        echo "Skipping Terraform Apply. Fetching outputs from existing state..."
                        // Ensure we have the state file or can access the backend
                        sh """
                            cd devops-managment/projects/domain-monitoring-system/terraform
                            terraform init -input=false
                            # We might need to refresh state if we want up-to-date outputs
                            terraform refresh \
                            -var="customer_name=${params.CUSTOMER_NAME}" \
                            -var="backend_instance_count=${params.BACKEND_COUNT}" \
                            -var="frontend_instance_count=${params.FRONTEND_COUNT}"
                        """
                    }

                    // Wrap the following Groovy logic in a `script` block so
                    // method calls and variable assignments are allowed.
                    
                    // 1. Get the JSON string from Terraform
                    def tfOutputString = sh(script: """
                        cd devops-managment/projects/domain-monitoring-system/terraform
                        terraform output -json
                    """, returnStdout: true).trim()

                    // 2. Parse the JSON string into a Groovy object
                    def outputs = readJSON text: tfOutputString

                    // 3. Loop through outputs and set them as global environment variables
                    outputs.each { key, data ->
                        // Convert key to UPPERCASE and extract the inner 'value'
                        env[key.toUpperCase()] = data.value
                    }
                }
            }
        }

        stage('Configure Backend') {
            steps {
                script {
                    runAnsibleOnIps(env.BACKEND_INSTANCE_PUBLIC_IPS, 'devops-managment/projects/domain-monitoring-system/ansible/configure_be.yaml')
                }
            }
        }

        stage('Backend Testing') {
            steps {
                script {
                    runAnsibleOnIps(env.BACKEND_INSTANCE_PUBLIC_IPS, 'devops-managment/projects/domain-monitoring-system/ansible/test_backend_api.yaml')
                }
            }
        }

        stage('Configure Frontend') {
            steps {
                script {
                    runAnsibleOnIps(env.FRONTEND_INSTANCE_PUBLIC_IPS, 'devops-managment/projects/domain-monitoring-system/ansible/configure_fe.yaml')
                }
            }
        }

        stage('Frontend Testing') {
            steps {
                script {
                    runAnsibleOnIps(env.FRONTEND_INSTANCE_PUBLIC_IPS, 'devops-managment/projects/domain-monitoring-system/ansible/test_frontend_selenium.yaml')
                }
            }
        }

    }
    
    post {
        always {
            script {
                // Email Notification
                def subject = "Deployment ${currentBuild.currentResult}: ${params.CUSTOMER_NAME}"
                def body = """
                    <h3>Deployment Status: ${currentBuild.currentResult}</h3>
                    <p><strong>Customer:</strong> ${params.CUSTOMER_NAME}</p>
                    <p><strong>Backend IPs:</strong> ${env.BACKEND_INSTANCE_PUBLIC_IPS}</p>
                    <p><strong>Frontend IPs:</strong> ${env.FRONTEND_INSTANCE_PUBLIC_IPS}</p>
                    <p><strong>Build URL:</strong> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                """

                emailext (
                    subject: subject,
                    body: body,
                    to: params.EMAIL_RECIPIENT,
                    mimeType: 'text/html'
                )

                // Cleanup workspace
                echo 'Cleaning up workspace...'
                sh 'rm -rf devops-managment'
                cleanWs()
            }
        }
    }
}