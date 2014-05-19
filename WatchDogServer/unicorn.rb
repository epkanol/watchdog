env = ENV['RUBY_ENV'] || 'production'
 
# 6 workers in production, 1 for development
worker_processes (env == 'production' ? 6 : 1)

timeout 30

preload_app true
 
