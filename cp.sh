echo "Press any key to copy additional binary files to $1/"
echo "Use Ctrl-C to cancel."
read
 
cp -v packages/apps/Settings/google-play-services.jar $1/packages/apps/Settings/
cp -v packages/apps/Settings/mapquest-android-sdk-?.?.?.jar $1/packages/apps/Settings/
find packages/apps/Settings/res ! -name "*.xml" -type f -exec cp -v --parents {} $1 \;

echo "Done."
