PACKAGE_NAME=stacksync
DIST_DIR=../../dist
VERSION=$(shell cat ../../version.properties | grep 'VERSION' | cut -d '=' -f 2)
INSTALLED_SIZE=$(shell du -sx $(DIST_DIR) | cut -f1)

all: compile package

.PHONY: clean

compile: 
	cd ../../; ant clean; ant

package: clean
	# Create package folder and folder structure
	mkdir -p $(PACKAGE_NAME)/usr/lib/stacksync/
	mkdir -p $(PACKAGE_NAME)/usr/bin
	mkdir -p $(PACKAGE_NAME)/usr/lib/nautilus/extensions-2.0
	mkdir -p $(PACKAGE_NAME)/usr/lib/nautilus/extensions-3.0
	mkdir -p $(PACKAGE_NAME)/usr/share/applications
	# Copy DEBIAN folder
	cp -r debian $(PACKAGE_NAME)/DEBIAN
	# Copy bin
	cp utils/stacksync $(PACKAGE_NAME)/usr/bin/
	# Copy the Dist folder
	cp -r $(DIST_DIR)/bin/ $(PACKAGE_NAME)/usr/lib/stacksync/
	cp -r $(DIST_DIR)/conf/ $(PACKAGE_NAME)/usr/lib/stacksync/
	cp -r $(DIST_DIR)/lib/ $(PACKAGE_NAME)/usr/lib/stacksync/
	cp -r $(DIST_DIR)/res/ $(PACKAGE_NAME)/usr/lib/stacksync/
	cp $(DIST_DIR)/Stacksync.jar $(PACKAGE_NAME)/usr/lib/stacksync/
	cp $(DIST_DIR)/UpdaterClient.jar $(PACKAGE_NAME)/usr/lib/stacksync/
	# Copy nautilus extension library
	cp utils/libnautilus-syncany.so $(PACKAGE_NAME)/usr/lib/nautilus/extensions-3.0/
	# Create symlink for nautilus extensions 2.0
	ln -s ../extensions-3.0/libnautilus-syncany.so $(PACKAGE_NAME)/usr/lib/nautilus/extensions-2.0/libnautilus-syncany.so
	# Copy menu entry
	cp utils/stacksync.desktop $(PACKAGE_NAME)/usr/share/applications/
	# Update version
	sed -i 's/VERSION/$(VERSION)/g' $(PACKAGE_NAME)/DEBIAN/control
	# Update package size
	sed -i 's/INSTALLED_SIZE/$(INSTALLED_SIZE)/g' $(PACKAGE_NAME)/DEBIAN/control
	# Generate md5sum file
	cd $(PACKAGE_NAME); find . -type f ! -regex '.*.hg.*' ! -regex '.*?debian-binary.*' ! -regex '.*?DEBIAN.*' -printf '%P ' | xargs md5sum > DEBIAN/md5sums
	# Create debian package
	dpkg --build $(PACKAGE_NAME) stacksync_$(VERSION)_all.deb
	

clean:
	rm -rf $(PACKAGE_NAME)
	


    
