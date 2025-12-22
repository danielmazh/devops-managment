// from this groovy i should get the following information:
//   -var="customer_name=client-x" \
//   -var="backend_instance_count=2" \
//   -var="frontend_instance_count=2"
// 

// to run manually tf:
//     cd projects/domain-monitoring-system/terraform
//     make sure ~/.aws/credentials

//     terraform init

//     terraform plan \
//     -var="customer_name=daniel-test" \
//     -var="backend_instance_count=1" \
//     -var="frontend_instance_count=2"

//     terraform apply \
//       -var="customer_name=daniel-test" \
//       -var="backend_instance_count=1" \
//       -var="frontend_instance_count=2" \
//       -auto-approve

pipeline {
  agent any
    parameters {
        string(name: 'PERSON_NAME', defaultValue: 'Anon', description: 'Who should we greet?')
        booleanParam(name: 'ENABLED', defaultValue: true, description: 'Is it enabled?')
    }
    
    stages {
    
        stage('BackEnd 1 - Terraform Configure and Apply') {
        steps {
            ssh
            """

            cd projects/domain-monitoring-system/terraform

            terraform init

            terraform plan \\
            -var="customer_name=${var.customer_name}" \\
            -var="backend_instance_count=1" \\
            -var="frontend_instance_count=2"

            terraform apply \\
            -var="customer_name=client-x" \\
            -var="backend_instance_count=2" \\
            -var="frontend_instance_count=2" \\
            -auto-approve
            
            """
        }
    }
  }
}


pipeline {
    agent any
    stages {
        stage('Dynamic Selection') {
            steps {
                script {
                    // 1. Load your list from a file or script
                    def listFromScript = ["A", "B", "C"] // Or load 'script.groovy'
                    
                    // 2. Prompt user mid-build
                    def selected = input message: 'Select version', parameters: [
                        choice(name: 'VERSION', choices: listFromScript.join('\n'))
                    ]
                    echo "User selected: ${selected}"
                }
            }
        }
    }
}