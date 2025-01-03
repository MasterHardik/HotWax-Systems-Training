## Swap File Setup (Linux)
This walks through the process of increasing your swap space by creating and enabling a **swap file**. The swap file is easy to manage and can be resized as needed. This guide assumes you're using a Linux system and the terminal.

### 1. Check Current Swap Usage
Before proceeding, it's good to check your current swap space:

```
bash
free -h
```

#### Example Output:
```
text
              total        used        free      shared  buff/cache   available
Mem:            15Gi       9.1Gi       1.2Gi       788Mi       5.0Gi       5.2Gi
Swap:          2.0Gi       1.7Gi       297Mi
```
In this case, the system has **2.0 GiB of swap**.

### 2. Create a 9 GB Swap File
You can create a swap file anywhere on your system. For this guide, we will place the swap file in the home directory (`/home/hardik`).

Command:
```
bash
sudo dd if=/dev/zero of=/home/hardik/swapfile bs=1M count=9216
```

- `if=/dev/zero`: Creates a file filled with zeros.
- `of=/home/hardik/swapfile`: Specifies the location of the swap file.
- `bs=1M`: Sets the block size to 1 MiB.
- `count=9216`: Creates a 9 GiB swap file (9216 blocks of 1 MiB).

#### Example Output:
```
text
9216+0 records in
9216+0 records out
9663676416 bytes (9.7 GB, 9.0 GiB) copied, 12.7995 s, 755 MB/s
```


### 3. Set Correct Permissions for the Swap File
For security reasons, set the correct permissions for the swap file so that only the root user can access it:

```
bash
sudo chmod 600 /home/hardik/swapfile
```

### 4. Set Up the Swap Space
Create the swap space from the file:

```
bash
sudo mkswap /home/hardik/swapfile
```

#### Example Output:

```
text
Setting up swapspace version 1, size = 9 GiB (9663672320 bytes)
no label, UUID=d3627c46-9638-45ca-9127-d86bdcb310cb
```

### 5. Activate the Swap File
Enable the swap file to be used by the system:

```
bash
sudo swapon /home/hardik/swapfile
```

### 6. Verify the Swap Space
Check that the swap space has been successfully activated:

```
bash
free -h
```

#### Example Output:

```
text
              total        used        free      shared  buff/cache   available
Mem:            15Gi       8.9Gi       437Mi       743Mi       6.0Gi       5.4Gi
Swap:          10Gi       2.0Gi       9.0Gi
```

You should see the swap space has increased to **10 GiB** (original 2 GiB + 9 GiB from the swap file).
Alternatively, you can also use the `swapon --show` command:

```
bash
swapon --show
```

This will list all active swap devices and files.

### 7. Make the Swap File Permanent
To ensure the swap file is enabled after every reboot, you need to add it to the `/etc/fstab` file.

1. Open the `/etc/fstab` file for editing:

```
bash
sudo nano /etc/fstab
```

2. Add the following line at the end of the file:

```
arduino
/home/hardik/swapfile none swap sw 0 0
```

3.Save and exit the editor (`Ctrl + X`, then `Y`, and hit `Enter`).

This will automatically activate the swap file at boot.

### 8. Optional: Adjust Swappiness (Optional)
You can adjust the swappiness value to control how aggressively the system uses swap space. A lower value means the system will prefer using RAM over swap.

#### Change swappiness temporarily:

```
bash
sudo sysctl vm.swappiness=10
```

#### Make swappiness permanent:
- Edit the /etc/sysctl.conf file:

  ```
  bash
  sudo nano /etc/sysctl.conf
  ```

- Add this line:
  ```
  text
  vm.swappiness=10
  ```

- Apply the changes:
  ```
  bash
  sudo sysctl -p
  ```

- Final Verification
After following these steps, you should have the following configuration:
  - **Total swap space**: 10 GiB (2 GiB original swap + 9 GiB swap file).
  - Active swap: The swap file at /home/hardik/swapfile is now active.
  - Automatic activation: The swap file will be activated on every boot.

Use `free -h` and `swapon --show` to verify your configuration.

#### Example Final Output of *free -h*:
```
vbnet
              total        used        free      shared  buff/cache   available
Mem:            15Gi       8.9Gi       437Mi       743Mi       6.0Gi       5.4Gi
Swap:          10Gi       2.0Gi       9.0Gi
```

#### Conclusion
You have successfully increased your swap space to 9 GiB by creating a swap file, activating it, and ensuring it is persistent across reboots. Additionally, you have the option to adjust swappiness to control when swap is used.
