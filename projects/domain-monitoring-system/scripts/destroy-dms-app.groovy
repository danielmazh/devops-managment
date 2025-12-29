// projects/domain-monitoring-system/scripts/destroy-dms-app.groovy

// Configure the job parameters using Active Choices Plugin
properties([
    parameters([
        string(name: 'CUSTOMER_NAME', description: 'Enter the Customer Environment to destroy (Based on existing Security Groups). Name should not include "-sg" suffix.', trim: true),
        booleanParam(name: 'CONFIRM_DELETION', defaultValue: false, description: 'SAFETY CHECK: Check this box to confirm you want to DESTROY the selected environment.')
    ])
])

pipeline {
    agent { label "jenkins-slave-team-3-new" }

    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    stages {
        stage('Safety Check') {
            steps {
                script {
                    if (!params.CONFIRM_DELETION) {
                        currentBuild.result = 'ABORTED'
                        error "DELETION ABORTED: You must check the CONFIRM_DELETION box to proceed."
                    }
                    if (!params.CUSTOMER_NAME) {
                        currentBuild.result = 'ABORTED'
                        error "INVALID SELECTION: Please enter a valid Customer Name."
                    }
                    echo "WARNING: You are about to DESTROY the environment for customer: ${params.CUSTOMER_NAME}"
                    echo "Waiting 5 seconds before proceeding..."
                    sleep 5
                }
            }
        }

        stage('Checkout Code') {
            steps {
                // Clean workspace to avoid conflicts
                sh '''
                    if [ -d devops-managment ]; then
                        rm -rf devops-managment
                    fi
                '''
                
                // Clone the repository
                withCredentials([usernamePassword(credentialsId: 'git-new-new', usernameVariable: 'gUser', passwordVariable: 'gPass')]) {
                    sh '''
                        git clone https://${gUser}:${gPass}@github.com/danielmazh/devops-managment.git
                    '''
                }
            }
        }

        stage('Terraform Destroy') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-creds',
                                                 usernameVariable: 'AWS_ACCESS_KEY_ID',
                                                 passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    script {
                        def awsRegion = env.AWS_DEFAULT_REGION ?: 'us-east-2'
                        def maxRetries = 3
                        def retryCount = 0
                        def success = false
                        
                        while (retryCount < maxRetries && !success) {
                            try {
                                sh """
                                    export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                                    export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                                    export AWS_DEFAULT_REGION=${awsRegion}

                                    cd devops-managment/projects/domain-monitoring-system/terraform
                                    
                                    echo "Initializing Terraform..."
                                    terraform init -input=false -backend-config="key=domain-monitoring/${params.CUSTOMER_NAME}.tfstate"
                                    
                                    echo "Attempting to acquire state lock (attempt \$((${retryCount} + 1))/${maxRetries})..."
                                    
                                    # Try to get the lock ID if state is locked
                                    LOCK_ID=\$(terraform plan -var="customer_name=${params.CUSTOMER_NAME}" 2>&1 | grep -oP 'ID:\\s+\\K[a-f0-9-]+' | head -1 || echo "")
                                    
                                    if [ -n "\$LOCK_ID" ]; then
                                        echo "State is locked with ID: \$LOCK_ID"
                                        echo "Attempting to force unlock..."
                                        terraform force-unlock -force "\$LOCK_ID"
                                    fi
                                    
                                    echo "Running Terraform Destroy for customer: ${params.CUSTOMER_NAME}"
                                    terraform destroy \
                                    -var="customer_name=${params.CUSTOMER_NAME}" \
                                    -auto-approve
                                """
                                success = true
                            } catch (Exception e) {
                                retryCount++
                                if (retryCount < maxRetries) {
                                    echo "Destroy attempt failed. Retrying in 10 seconds... (${retryCount}/${maxRetries})"
                                    sleep 10
                                } else {
                                    echo "All retry attempts exhausted. Manual intervention may be required."
                                    throw e
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    post {
        always {
            deleteDir()
        }
        success {
            echo "Environment for ${params.CUSTOMER_NAME} has been successfully destroyed."
        }
        failure {
            echo "Destruction failed. Please check the logs."
        }
    }
}

