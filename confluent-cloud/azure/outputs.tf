output "resource-ids" {
  value = <<-EOT

  Resource Group:      ${azurerm_resource_group.rg.name}
  Connect to Jumpbox:  ssh -i ${local_file.private_key.filename} ${azurerm_linux_virtual_machine.vm.admin_username}@${azurerm_linux_virtual_machine.vm.public_ip_address}

  EOT
  sensitive = true
}

