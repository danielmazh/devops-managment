// from this groovy i should get the following information:
//   -var="customer_name=client-x" \
//   -var="backend_instance_count=2" \
//   -var="frontend_instance_count=2"

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