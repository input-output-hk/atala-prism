#!/bin/bash
set -euo pipefail
# Name of the resource to copy
INFO_PLIST_FILE=GoogleService-Info.plist

# Get references to debug and release versions of the plist file
DEBUG_INFO_PLIST_FILE=${SRCROOT}/FirebaseConfigurations/debug/${INFO_PLIST_FILE}
RELEASE_INFO_PLIST_FILE=${SRCROOT}/FirebaseConfigurations/release/${INFO_PLIST_FILE}

# Make sure the debug version exists
echo "Looking for ${INFO_PLIST_FILE} in ${DEBUG_INFO_PLIST_FILE}"
if [ ! -f "$DEBUG_INFO_PLIST_FILE" ] ; then
    echo "File GoogleService-Info.plist (testing) not found."
    exit 1
fi

# Make sure the release version exists
echo "Looking for ${INFO_PLIST_FILE} in ${RELEASE_INFO_PLIST_FILE}"
if [ ! -f "$RELEASE_INFO_PLIST_FILE" ] ; then
    echo "File GoogleService-Info.plist (release) not found."
    exit 1
fi

# Get a reference to the destination location for the plist file
PLIST_DESTINATION=${BUILT_PRODUCTS_DIR}/${PRODUCT_NAME}.app
echo "Copying ${INFO_PLIST_FILE} to final destination: ${PLIST_DESTINATION}"

# Copy the appropiate file to app bundle
if [ "${CONFIGURATION}" == "Debug" ] ; then
    echo "File ${DEBUG_INFO_PLIST_FILE} copied"
    cp "${DEBUG_INFO_PLIST_FILE}" "${PLIST_DESTINATION}"
else
    echo "File ${RELEASE_INFO_PLIST_FILE} copied"
    cp "${RELEASE_INFO_PLIST_FILE}" "${PLIST_DESTINATION}"
fi
