# Introduction & Usage

Log Parser for Multilines Event.

## 1.Prerequisites

* Java 1.8
* Maven 3.0+

## 2.Build

`mvn clean package`

## 3.Run the Demos

### For MS-Windows

```sh
cd scripts
.\healthApp-LogDemo.bat
.\openstack-LogDemo.bat
.\windows-LogDemo.bat
```

### For Linux

Firstly, assign execute permission:

```sh
cd scripts
chmod +x healthApp-LogDemo.sh openstack-LogDemo.sh windows-LogDemo.sh
```

Then, execute the scripts:

```sh
.\healthApp-LogDemo.sh
.\openstack-LogDemo.sh
.\windows-LogDemo.sh
```

Those demos have been tested in MS_Windows 10 and CentOS 7.