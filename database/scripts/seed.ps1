# Retired in T-021 (ADR-024). The PowerShell version had a default database host, user and password,
# passed the password through MYSQL_PWD and read ..\..\.env.local implicitly. It was not ported because
# it could not be tested; use the bash script, which enforces the T-021 rules (explicit connection,
# --defaults-extra-file, target confirmation). On Windows, run it from Git Bash or WSL:
#
#   bash database/scripts/seed.sh --env-file <path>
#
Write-Error "seed.ps1 was retired in T-021. Use: bash database/scripts/seed.sh --env-file <path> (Git Bash or WSL). See database/README.md."
exit 1
