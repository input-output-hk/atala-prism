output "authorized_keys" {
  description = "Content of authorized_keys file"
  value       = file("${path.module}/authorized_keys")
}