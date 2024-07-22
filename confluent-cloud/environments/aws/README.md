### Configure CLI

```bash
$ aws configure
$ aws configure set region eu-west-1
$ aws configure list

      Name                    Value             Type    Location
      ----                    -----             ----    --------
   profile                <not set>             None    None
access_key     ****************L5AD shared-credentials-file
secret_key     ****************Lhpq shared-credentials-file
    region                eu-west-1      config-file    ~/.aws/config
```

Set up your `AWS` access in your local environment, so that the command

More info here: https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html

```bash
$ aws sts get-caller-identity
```

gives you a positive response.

Set up `terraform.tfvars` to your liking, then run the usual

```bash
$ terraform init
$ terraform plan
$ terraform apply

```

Login to jump host:

```bash
$ terraform output | grep jumphost-ip
$ ssh -i bootcamp.pem ubuntu@<jumphost-ip>
```


