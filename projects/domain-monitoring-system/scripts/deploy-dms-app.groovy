import groovy.json.JsonSlurper

// Helper that performs the Docker Hub API calls and returns a list of tag names.
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

// Top-level helper used by the pipeline parameters. It prefers using Jenkins
// `withCredentials` if available; if not (e.g. during parameter evaluation in
// some environments), it falls back to environment variables `DOCKERHUB_USER`
// and `DOCKERHUB_PASS`.
def getTags(String repoName) {
    try {
        // If the pipeline runtime provides the `withCredentials` step, use it.
        return withCredentials([usernamePassword(credentialsId: 'RonDockerUser', usernameVariable: 'dUser', passwordVariable: 'dPass')]) {
            return fetchDockerHubTags(repoName, dUser, dPass)
        }
    } catch (MissingMethodException | NoSuchMethodError e) {
        // `withCredentials` may not be available at parse time; try environment fallback.
        def envUser = System.getenv('DOCKERHUB_USER') ?: ''
        def envPass = System.getenv('DOCKERHUB_PASS') ?: ''
        return fetchDockerHubTags(repoName, envUser, envPass)
    }
}

pipeline {
    agent {label "jenkins-slave-team-3-new"}

    parameters {
        // Note: The 'getTags' function runs on the Jenkins Controller when the pipeline is loaded.
        // If security sandbox is enabled, you may need to approve the signatures in "In-process Script Approval".
        // Replace 'mailguard-platform' with your actual repository name or make it dynamic if feasible.
        choice(name: 'BACKEND_VERSION', choices: getTags("dms-be"), description: 'Select a tag from Docker Hub')
        choice(name: 'FRONTEND_VERSION', choices: getTags("dms-fe"), description: 'Select a tag from Docker Hub')

        string(name: 'CUSTOMER_NAME', defaultValue: 'test_customer', description: 'Enter customer name')
        choice(name: 'FRONTEND_COUNT', choices: ['1','2','3'], description: 'Number of frontend instances')
        choice(name: 'BACKEND_COUNT', choices: ['1'], description: 'Number of backend instances')
    }

    stages {
        stage('Check Params') {
            steps {
                echo "Selected Backend Version is: ${params.BACKEND_VERSION}"
                echo "Selected Frontend Version is: ${params.FRONTEND_VERSION}"
                echo "Name: ${params.CUSTOMER_NAME}"
                echo "Frontend Instances: ${params.FRONTEND_COUNT}"
                echo "Backend Instances: ${params.BACKEND_COUNT}"
            }
        }
        stage('Checkout Code') {
            steps {
                // Remove any leftover clone from previous runs to avoid "destination path already exists"
                sh '''
                    if [ -d devops-managment ]; then
                        rm -rf devops-managment
                    fi
                '''
                withCredentials([usernamePassword(credentialsId: 'RonGitUser', usernameVariable: 'gUser', passwordVariable: 'gPass')])
                {
                    sh '''
                        rm -rf devops-managment
                        git clone https://${gUser}:${gPass}@github.com/danielmazh/devops-managment.git
                    '''
                }
            }
        }
        stage('Trigger iac') {
            steps {
                // 1. Init and Plan
                sh """
                    pwd
                    cd devops-managment/projects/domain-monitoring-system/terraform
                    pwd
                    ls -la
                    
                    terraform init
                    
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

                // Wrap the following Groovy logic in a `script` block so
                // method calls and variable assignments are allowed.
                script {
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
                 echo 'Hello World'
            }
        }
        stage('Backend Testing') {
            steps {
                echo 'Hello World'
            }
        }
        stage('Configure Frontend') {
            steps {
                 echo 'Hello World'

            }
        }
        stage('Frontend Testing') {
            steps {
                 echo 'Hello World'
            }
        }
        stage('Notify User') {
            steps {
                 echo 'Hello World'
            }
        }
        stage('Cleanup') {
            steps {
                 echo 'Hello World' 
            }
        }
    }
}