START_DIR=$(pwd)

cd "$(dirname "$0")" || exit 1
cp -p ../target/wwf.jar artifact
cp -p ../target/newrelic/newrelic.jar artifact
cp -p ../target/newrelic/newrelic.yml artifact
cp -rp ../.ebextensions artifact

NEW_RELIC_APP_NAME="WWF Server"
NEW_RELIC_LICENSE_KEY=$(aws secretsmanager get-secret-value --secret-id NewRelicLicenseKey | jq -r '.SecretString' | jq -r '.value')
sed -ie "s/  app_name: My Application/  app_name: ${NEW_RELIC_APP_NAME}/" artifact/newrelic.yml
sed -ie "s/<%= license_key %>/${NEW_RELIC_LICENSE_KEY}/" artifact/newrelic.yml
rm artifact/newrelic.ymle

cd artifact || exit 1
zip -r wwf.zip .
mv wwf.zip ..

cd "${START_DIR}" || exit 1
