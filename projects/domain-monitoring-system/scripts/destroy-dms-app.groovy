// projects/domain-monitoring-system/scripts/destroy-dms-app.groovy

// Configure the job parameters using Active Choices Plugin
properties([
    parameters([
        [
            $class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            name: 'CUSTOMER_NAME',
            description: 'Select the Customer Environment to destroy (Based on existing Security Groups)',
            filterable: true,
            filterLength: 1,
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return ["ERROR: Could not fetch customers"]'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: '''
                        try {
                            // Execute AWS CLI to list security groups
                            // This runs on the Jenkins Controller (Master)
                            def cmd = ["/bin/bash", "-c", "aws ec2 describe-security-groups --query 'SecurityGroups[*].GroupName' --output text"]
                            def process = cmd.execute()
                            process.waitFor()
                            
                            if (process.exitValue() != 0) {
                                return ["Error: AWS CLI failed with exit code " + process.exitValue()]
                            }
                            
                            def output = process.text
                            if (!output) return ["No security groups found"]
                            
                            // Parse output (tab-separated), find items ending in "-sg", and remove the suffix
                            def customers = output.split('\\t')
                                .findAll { it.endsWith("-sg") }
                                .collect { it.replace("-sg", "") }
                                .unique()
                                .sort()
                                
                            return customers
                        } catch (Exception e) {
                            return ["Exception: " + e.message]
                        }
                    '''
                ]
            ]
        ],
        booleanParam(name: 'CONFIRM_DELETION', defaultValue: false, description: 'SAFETY CHECK: Check this box to confirm you want to DESTROY the selected environment.')
    ])
])

pipeline {
    agent { label "jenkins-slave-team-3-new" }

    options {
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    stages {
        stage('Safety Check') {
            steps {
                script {
                    if (!params.CONFIRM_DELETION) {
                        currentBuild.result = 'ABORTED'
                        error "⛔ DELETION ABORTED: You must check the CONFIRM_DELETION box to proceed."
                    }
                    if (!params.CUSTOMER_NAME || params.CUSTOMER_NAME.startsWith("ERROR") || params.CUSTOMER_NAME.startsWith("Error")) {
                        currentBuild.result = 'ABORTED'
                        error "⛔ INVALID SELECTION: Please select a valid Customer Name."
                    }
                    echo "⚠️ WARNING: You are about to DESTROY the environment for customer: ${params.CUSTOMER_NAME}"
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
                withCredentials([usernamePassword(credentialsId: 'RonGitUser', usernameVariable: 'gUser', passwordVariable: 'gPass')]) {
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
                        // Initialize and Destroy
                        sh """
                            export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                            export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                            export AWS_DEFAULT_REGION=${awsRegion}

                            cd devops-managment/projects/domain-monitoring-system/terraform
                            
                            echo "Initializing Terraform..."
                            terraform init -input=false
                            
                            echo "Running Terraform Destroy for customer: ${params.CUSTOMER_NAME}"
                            terraform destroy \
                            -var="customer_name=${params.CUSTOMER_NAME}" \
                            -auto-approve
                        """
                    }
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo "Environment for ${params.CUSTOMER_NAME} has been successfully destroyed."
        }
        failure {
            echo "Destruction failed. Please check the logs."
        }
    }
}

