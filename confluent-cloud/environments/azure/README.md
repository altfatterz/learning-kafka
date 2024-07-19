Set up your `Azure` access in your local environment, so that the command

        aws sts get-caller-identity

gives you a positive response.

Set up `terraform.tfvars` to your liking, then run the usual

        terraform init
        terraform plan
        terraform apply


Connect to `jumpbox`: 

        terraform output
        terraform output resource-ids
        
Cleanup:
        
        terraform 
