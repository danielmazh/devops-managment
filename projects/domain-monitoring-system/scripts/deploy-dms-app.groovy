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
        return withCredentials([usernamePassword(credentialsId: 'docker-hub-ron-token', usernameVariable: 'dUser', passwordVariable: 'dPass')]) {
            return fetchDockerHubTags(repoName, dUser, dPass)
        }
    } catch (MissingMethodException | NoSuchMethodError e) {
        // `withCredentials` may not be available at parse time; try environment fallback.
        def envUser = System.getenv('DOCKERHUB_USER') ?: ''
        def envPass = System.getenv('DOCKERHUB_PASS') ?: ''
        return fetchDockerHubTags(repoName, envUser, envPass)
    }
}

// Helper that runs an Ansible playbook on a list of IPs using Docker
def runAnsibleOnIps(String ipsJson, String playbookPath, String extraVars = "") {
    def ips = []
    try {
        if (ipsJson instanceof String) {
            // Check if it's already a string representation of a list
            if (ipsJson.startsWith("[") && ipsJson.endsWith("]")) {
                ips = new groovy.json.JsonSlurper().parseText(ipsJson)
            } else {
                 // Assume single IP string if not json array
                 ips = [ipsJson]
            }
        } else if (ipsJson instanceof List) {
             ips = ipsJson
        }
    } catch (Exception e) {
        echo "Error parsing IPs JSON: ${e.message}. Input was: ${ipsJson}"
        // Fallback or re-throw
        return 
    }
    
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
                chmod 600 \$SSH_KEY
                docker run --rm \
                  -v \$SSH_KEY:/ssh-key:ro \
                  -v \${WORKSPACE}:/workspace:ro \
                  -e ANSIBLE_HOST_KEY_CHECKING=False \
                  cytopia/ansible:latest \
                  ansible-playbook -i '${ip},' -u ubuntu --private-key /ssh-key /workspace/${playbookPath} ${extraVars}
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
        booleanParam(name: 'AUTO_APPROVE', defaultValue: false, description: 'Skip manual approval and auto-apply Terraform')
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
                    withCredentials([usernamePassword(credentialsId: 'aws-creds',
                                                     usernameVariable: 'AWS_ACCESS_KEY_ID',
                                                     passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {

                        def awsRegion = env.AWS_DEFAULT_REGION ?: 'us-east-2'

                        if (!params.SKIP_TERRAFORM) {
                            // 1. Init and Plan
                            sh """
                                export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                                export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                                export AWS_DEFAULT_REGION=${awsRegion}

                                cd devops-managment/projects/domain-monitoring-system/terraform
                                terraform init -input=false -backend-config="key=domain-monitoring/${params.CUSTOMER_NAME}.tfstate"
                                terraform plan \
                                  -var="customer_name=${params.CUSTOMER_NAME}" \
                                  -var="backend_instance_count=${params.BACKEND_COUNT}" \
                                  -var="frontend_instance_count=${params.FRONTEND_COUNT}"
                            """

                            // 2. Pause for User Validation unless auto-approval is enabled
                            if (!params.AUTO_APPROVE) {
                                input message: 'Review the plan above. Approve Apply?', ok: 'Deploy'
                            } else {
                                echo 'AUTO_APPROVE=true: skipping manual approval gate.'
                            }

                            // 3. Apply (Keep -auto-approve so the command doesn't hang)
                            sh """
                                export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                                export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                                export AWS_DEFAULT_REGION=${awsRegion}

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
                                export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                                export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                                export AWS_DEFAULT_REGION=${awsRegion}

                                cd devops-managment/projects/domain-monitoring-system/terraform
                                terraform init -input=false -backend-config="key=domain-monitoring/${params.CUSTOMER_NAME}.tfstate"
                                # We might need to refresh state if we want up-to-date outputs
                                terraform refresh \
                                  -var="customer_name=${params.CUSTOMER_NAME}" \
                                  -var="backend_instance_count=${params.BACKEND_COUNT}" \
                                  -var="frontend_instance_count=${params.FRONTEND_COUNT}"
                            """
                        }
                    }

                    // Wrap the following Groovy logic in a `script` block so
                    // method calls and variable assignments are allowed.
                    
                    // 1. Get the JSON string from Terraform
                    def tfOutputString = ""
                    withCredentials([usernamePassword(credentialsId: 'aws-creds',
                                                     usernameVariable: 'AWS_ACCESS_KEY_ID',
                                                     passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                        def awsRegion = env.AWS_DEFAULT_REGION ?: 'us-east-2'
                        
                        tfOutputString = sh(script: """
                            export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                            export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                            export AWS_DEFAULT_REGION=${awsRegion}

                            cd devops-managment/projects/domain-monitoring-system/terraform
                            terraform output -json > output.json
                            cat output.json
                        """, returnStdout: true).trim()
                    }

                    // 2. Parse the JSON string into a Groovy object
                    // def outputs = readJSON text: tfOutputString
                    // Parse the JSON string from the file directly to avoid shell echo issues if possible, 
                    // but we are reading the cat output. 
                    // Let's ensure tfOutputString is clean.
                     def outputs = new JsonSlurper().parseText(tfOutputString)

                    // 3. Loop through outputs and set them as global environment variables
                    outputs.each { key, data ->
                        // Convert key to UPPERCASE and extract the inner 'value'
                        // Ensure we are handling the value correctly. Terraform output json has "value" key.
                        def val = data.value
                        // If it's a list, convert to JSON string so we can parse it back later
                        if (val instanceof List) {
                            env[key.toUpperCase()] = groovy.json.JsonOutput.toJson(val)
                        } else {
                            env[key.toUpperCase()] = val.toString()
                        }
                    }
                }
            }
        }

        stage('Configure Backend') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-ron-token', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
                        def extraVars = "-e 'docker_user=${DOCKER_USER} docker_password=${DOCKER_PASSWORD} backend_version=${params.BACKEND_VERSION}'"
                        runAnsibleOnIps(env.BACKEND_INSTANCE_PUBLIC_IPS, 'devops-managment/projects/domain-monitoring-system/ansible/configure_be.yaml', extraVars)
                    }
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
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-ron-token', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
                        def extraVars = "-e 'docker_user=${DOCKER_USER} docker_password=${DOCKER_PASSWORD} frontend_version=${params.FRONTEND_VERSION}'"
                        runAnsibleOnIps(env.FRONTEND_INSTANCE_PUBLIC_IPS, 'devops-managment/projects/domain-monitoring-system/ansible/configure_fe.yaml', extraVars)
                    }
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
                deleteDir()
            }
        }
    }
}