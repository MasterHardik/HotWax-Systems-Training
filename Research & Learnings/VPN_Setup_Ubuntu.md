# VPN Setup on Ubuntu Linux

This guide provides step-by-step instructions on how to set up a VPN (Virtual Private Network) on Ubuntu Linux. The instructions use the built-in **NetworkManager** tool, which makes the setup process easier.

## Prerequisites

1. A running installation of Ubuntu Linux (tested on versions 18.04, 20.04, 22.04).
2. A valid VPN provider (either private or commercial).
3. Access credentials (username, password, and/or certificate) provided by your VPN provider.
4. Administrative privileges on the system.

## Step 1: Install Required Packages

To use the VPN, you need to install certain packages that support VPN protocols such as OpenVPN, IKEv2, or PPTP.

1. Open a terminal window and run the following command to update your package list:

    ```bash
    sudo apt update
    ```

2. Install the necessary packages for OpenVPN and IKEv2 (adjust if you're using another VPN protocol):

    ```bash
    sudo apt install network-manager-openvpn network-manager-openvpn-gnome
    ```

    **Note:** If you are using a different VPN type (such as IKEv2), you may need to install `network-manager-strongswan` for IKEv2, or `network-manager-pptp` for PPTP.

3. Restart NetworkManager to ensure all changes are applied:

    ```bash
    sudo systemctl restart NetworkManager
    ```

## Step 2: Configure VPN Using GUI

1. Click on the network icon in the top-right corner of the screen (next to your Wi-Fi or Ethernet status).

2. Select **"VPN Off"** or **"Network Settings"**.

3. In the **Network Settings** window, click the **"+"** button at the bottom left to add a new connection.

4. In the pop-up window, select **VPN** from the list of connection types, then choose your VPN type. For this example, we'll assume you're using **OpenVPN**.

    - **If using OpenVPN**: Choose **OpenVPN**.
    - **If using IKEv2**: Choose **IPsec IKEv2**.
    - **If using PPTP**: Choose **PPTP**.

5. Enter the VPN connection details:
   - **Connection Name**: Choose a name for the VPN connection.
   - **Gateway**: Enter the VPN server address (e.g., `vpn.example.com`).
   - **Authentication**: Enter your VPN provider's authentication details (username/password or certificate).

    For OpenVPN:
    - **User Name**: Your VPN username.
    - **Password**: Your VPN password (optional: you can store the password securely).
    - **CA Certificate**: If your VPN requires a certificate, download it from the VPN provider and upload it here.

6. After entering the required information, click **Apply** to save the connection.

## Step 3: Connect to the VPN

1. After the VPN connection is saved, click on the network icon in the system tray again.

2. You should now see the new VPN connection listed under **VPN Connections**.

3. Select your VPN connection to start the VPN.

4. If the connection is successful, the network icon will change to indicate the active VPN connection.

## Step 4: Verify the VPN Connection

To verify your VPN connection:

1. Open a terminal window.

2. Use the `curl` or `wget` command to check your public IP address. If the VPN is active, the IP address should be that of the VPN server.

    ```bash
    curl ifconfig.me
    ```

    This should return the IP address of the VPN server you're connected to.

3. Optionally, you can also use `ip a` to check the VPN interface (typically `tun0` for OpenVPN).

    ```bash
    ip a
    ```

    Look for an interface named `tun0` (or another name depending on the VPN) with an IP address assigned.

## Step 5: Disconnect from the VPN

To disconnect from the VPN:

1. Click on the network icon in the top-right corner again.

2. Select **"Disconnect"** from the VPN connection.

## Troubleshooting

If you're having issues connecting, here are a few tips:

- **Check your credentials**: Make sure that the username, password, and server address are correct.
- **Check the VPN logs**: To view detailed connection logs, open the terminal and run the following command:

    ```bash
    journalctl -u NetworkManager
    ```

- **Reinstall NetworkManager**: If you experience persistent issues, you can try reinstalling the `network-manager` package.

    ```bash
    sudo apt reinstall network-manager
    ```

## Free VPN: How to Get a Free VPN Using ProtonVPN on Ubuntu

If you're looking for a **free VPN service**, ProtonVPN offers a free tier that you can set up on Ubuntu. Follow these steps to install and configure ProtonVPN.

### Step 1: Visit the ProtonVPN Website

1. Go to the official ProtonVPN download page:  
   [ProtonVPN Downloads](https://account.protonvpn.com/downloads)

2. Select the **Linux** option to access the download page:  
   [ProtonVPN Linux Setup](https://protonvpn.com/download-linux)

### Step 2: Download ProtonVPN for Ubuntu

1. Click on **Install ProtonVPN** to view installation instructions for Ubuntu:  
   [ProtonVPN Linux Installation Guide](https://protonvpn.com/support/linux-vpn-setup/)

2. Find instructions for Ubuntu GNOME, the recommended installation method:  
   [ProtonVPN Ubuntu GNOME Setup](https://protonvpn.com/support/official-linux-vpn-ubuntu/)

### Step 3: Install ProtonVPN on Ubuntu

1. Open a terminal window and run the following commands to download and install ProtonVPN:

    ```bash
    wget https://repo.protonvpn.com/debian/dists/stable/main/binary-all/protonvpn-stable-release_1.0.6_all.deb
    sudo dpkg -i ./protonvpn-stable-release_1.0.6_all.deb && sudo apt update
    ```

2. Verify the download package:

    ```bash
    echo "e5e03976d0980bafdf07da2f71b14fbc883c091e72b16772199742c98473002f protonvpn-stable-release_1.0.6_all.deb" | sha256sum --check -
    ```

3. Install ProtonVPN client:

    ```bash
    sudo apt install proton-vpn-gnome-desktop
    ```

4. Install dependencies:

    ```bash
    sudo apt install libayatana-appindicator3-1 gir1.2-ayatanaappindicator3-0.1 gnome-shell-extension-appindicator
    ```

### Step 4: Launch ProtonVPN

1. Once installed, search for **ProtonVPN** in the application finder or use the command line to launch it.

2. Open the application and log in using the email and password you created when signing up for ProtonVPN.

### Step 5: Connect to ProtonVPN

1. After logging in, you can select the server you wish to connect to and start using ProtonVPN.

2. You can now access the internet securely and privately.

---

## Conclusion

This guide has shown you how to set up a VPN on Ubuntu using both the **NetworkManager GUI** for traditional VPN services and **ProtonVPN**
