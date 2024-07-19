variable "rg-name" {
  default     = "my-resource-group"
  description = "Name of the resource group."
}

variable "rg-location" {
  default     = "westeurope"
  description = "Location of the resource group."
}

variable "vnet-cidr" {
  default = "10.0.0.0/16"
  description = "The CIDR range for the VNet"
}

variable "subnet-cidr" {
  default = "subnet-cidr"
  description = "The CIDR range for the subnet"
}

variable "hostname" {
  default     = "my-host"
  description = "Specifies the Hostname which should be used for this Virtual Machine."
}

variable "admin-username" {
  default     = "ubuntu"
  description = "The username of the local administrator used for the Virtual Machine."
}