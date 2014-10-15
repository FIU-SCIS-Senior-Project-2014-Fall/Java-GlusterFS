#Setting Up

You must be on a 64-bit Linux machine (it will not work on a VM, I don't think). Your CPU MUST have 64-bit virtualization technology and that must be enabled in the BIOS.

* go to virtualbox.org and download latest
* go to vagrantup.com and download latest
* go to oracle.com and download jdk7
* unpack jdk into /home/{name}/jdk/
* go to intellij's site and download latest IDEA
* create script to load IDEA and set IDEA_JDK env-var to point to /home/{name}/jdk/...
* create desktop entry and point it at script
* sudo apt-get install git
* load IntelliJ to create IdeaProjects folder
* download lombok plugin
* load plugin into IntelliJ
* clone repo into IdeaProjects

In one terminal:

    sudo apt-get install libtool build-essential pkg-config automake default-jdk maven glusterfs-common
    export JAVA_HOME=/usr/lib/jvm/default-java
    cd ~/IdeaProjects/Java-GlusterFS/glusterfs-java-filesystem
    vagrant up
    vagrant ssh
    sudo -i
    add-apt-repository -y ppa:gluster/glusterfs-3.4
    apt-get update
    apt-get install glusterfs-server
    sed -i 's/\(end-volume\)/    option rpc-auth-allow-insecure on\n\1/' /etc/glusterfs/glusterd.vol
    service glusterfs-server restart
    gluster volume create foo 172.31.31.31:/var/tmp/foo force
    gluster volume set foo server.allow-insecure on
    gluster volume start foo
    mkdir /mnt/foo
    mount -t glusterfs localhost:foo /mnt/foo
    chmod ugo+w
    su vagrant

In another terminal:

    cd /home/{name}/IDEA/IdeaProjects/Java-GlusterFS/libgfapi-jni
    mvn -P download install
    mvn -P linux64 clean install
    cd ../glusterfs-java-filesystem
    mvn clean install
    cd gluster-java-filesystem-example
    mvn exec:exec

##Concerns

* If no permissions are specified, creation of a directory will default to rwxrwxr-x. The default behavior is concerned with the umask and changes the default permission set based on the formula 0777 & ~umask. We don't feel this is an imperative difference in behavior, so we are leaving it a a potential to do later.
