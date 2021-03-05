output "envoy_lb_dns_name" {
  description = "DNS name of AWS LB pointing to Envoy"
  value       = module.prism_lb.this_lb_dns_name
}
