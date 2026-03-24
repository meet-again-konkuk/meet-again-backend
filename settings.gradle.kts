rootProject.name = "meet-again"

include("boot:ma-boot-web")
include("boot:ma-boot-batch")
include("domain:ma-domain-core")
include("infrastructure:storage:ma-db-core")
include("infrastructure:storage:ma-redis-core")
include("infrastructure:support:ma-sms-sender")
include("infrastructure:support:ma-crypto-core")
include("infrastructure:support:ma-jwt-core")
include("infrastructure:support:ma-file-storage")
include("config:ma-config-yaml-importer")
include("config:ma-config-logging")
