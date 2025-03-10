rootProject.name = "meet-again"

include("boot:ma-boot-web")
include("domain:ma-domain-core")
include("storage:ma-db-core")
include("storage:ma-redis-core")
include("support:ma-sms-sender")
include("config:ma-config-yaml-importer")
