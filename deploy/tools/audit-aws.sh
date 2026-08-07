#!/bin/sh

set -eu

: "${AWS_REGION:?AWS_REGION is required}"
: "${EC2_INSTANCE_ID:?EC2_INSTANCE_ID is required}"
: "${RDS_INSTANCE_ID:?RDS_INSTANCE_ID is required}"

command -v aws >/dev/null 2>&1 || {
    echo "AWS CLI is required" >&2
    exit 1
}

ec2_metadata=$(aws ec2 describe-instances \
    --region "$AWS_REGION" \
    --instance-ids "$EC2_INSTANCE_ID" \
    --query 'Reservations[0].Instances[0].MetadataOptions' \
    --output json)
rds_metadata=$(aws rds describe-db-instances \
    --region "$AWS_REGION" \
    --db-instance-identifier "$RDS_INSTANCE_ID" \
    --query 'DBInstances[0].{PubliclyAccessible:PubliclyAccessible,StorageEncrypted:StorageEncrypted,DeletionProtection:DeletionProtection,BackupRetentionPeriod:BackupRetentionPeriod,AutoMinorVersionUpgrade:AutoMinorVersionUpgrade,Status:DBInstanceStatus}' \
    --output json)

printf '%s\n' "$ec2_metadata" | jq -e '
    .HttpTokens == "required" and .HttpEndpoint == "enabled"
' >/dev/null || {
    echo "fail EC2 metadata service must require IMDSv2 tokens" >&2
    exit 1
}
echo "ok EC2 IMDSv2"

printf '%s\n' "$rds_metadata" | jq -e '
    .PubliclyAccessible == false and
    .StorageEncrypted == true and
    .DeletionProtection == true and
    .BackupRetentionPeriod >= 1 and
    .Status == "available"
' >/dev/null || {
    echo "fail RDS must be private, encrypted, deletion-protected, available, and backed up" >&2
    printf '%s\n' "$rds_metadata" >&2
    exit 1
}
echo "ok RDS private/encrypted/deletion-protected/backup"
