package com.cankurttekin.andmenu.commandSearchers.eachSearcher;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.cankurttekin.andmenu.R;
import com.cankurttekin.andmenu.applicationMain.MainActivity;
import com.cankurttekin.andmenu.commandSearchers.lib.StringMatchStrategy;
import com.cankurttekin.andmenu.commands.netutils.PingActivity;
import com.cankurttekin.andmenu.commands.netutils.Ping6Activity;
import com.cankurttekin.andmenu.interfaces.CandidateEntry;
import com.cankurttekin.andmenu.interfaces.CommandSearcher;
import com.cankurttekin.andmenu.interfaces.EventLauncher;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetUtilCommandSearcher implements CommandSearcher {
    private final boolean _pingAvailable;
    private final boolean _ping6Available;

    private static final Map<String, List<String>> dnsCache = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> isFetchingDns = new ConcurrentHashMap<>();

    public NetUtilCommandSearcher() {
        this._pingAvailable = PingActivity.commandAvailable();
        this._ping6Available = Ping6Activity.commandAvailable();
    }

    @Override
    public void refresh(Context context) {}

    @Override
    public void close() {}

    @Override
    public boolean isPrepared() {
        return true;
    }

    @Override
    public void waitUntilPrepared() {}

    @Override
    @NonNull
    public List<CandidateEntry> searchCandidateEntries(String s, Context context) {
        List<CandidateEntry> ret = new ArrayList<>();

        String trimmed = s.trim();
        String lower = trimmed.toLowerCase();

        // Must start with "ip " or be "ip " exactly
        if (s.startsWith("ip ") || lower.equals("ip ")) {
            List<Pair<String, String>> localIps = getFilteredLocalIpAddresses();
            String ssid = getWifiSSID(context);
            List<Pair<String, String>> macs = getMacAddresses();
            ret.add(new NetUtilIpCandidateEntry(localIps, ssid, macs));
            return ret;
        }

        if (lower.equals("dns")) {
            List<String> systemDns = getSystemDnsServers(context);
            for (String dns : systemDns) {
                ret.add(new CopyToClipboardCandidateEntry("System DNS Server", dns));
            }
            ret.add(new CopyToClipboardCandidateEntry("DNS Lookup", "Usage: dns <domain>", false));
            return ret;
        }

        if (lower.startsWith("dns ")) {
            String domain = trimmed.substring(4).trim();
            if (!domain.isEmpty()) {
                if (dnsCache.containsKey(domain)) {
                    List<String> ips = dnsCache.get(domain);
                    if (ips != null && !ips.isEmpty()) {
                        for (String resolvedIp : ips) {
                            ret.add(new CopyToClipboardCandidateEntry("DNS (" + domain + ")", resolvedIp));
                        }
                    } else {
                        ret.add(new CopyToClipboardCandidateEntry("DNS (" + domain + ")", "No IP records found", false));
                    }
                } else {
                    fetchDnsAsync(domain);
                    ret.add(new CopyToClipboardCandidateEntry("DNS (" + domain + ")", "Resolving... (tap to retry)", false));
                }
            } else {
                ret.add(new CopyToClipboardCandidateEntry("DNS Lookup", "Usage: dns <domain>", false));
            }
            return ret;
        }

        if (lower.equals("ping")) {
            if (this._pingAvailable) {
                ret.add(new CopyToClipboardCandidateEntry("Ping Command", "Usage: ping <host>", false));
            }
            return ret;
        }

        if (lower.equals("ping6")) {
            if (this._ping6Available) {
                ret.add(new CopyToClipboardCandidateEntry("Ping6 Command", "Usage: ping6 <host>", false));
            }
            return ret;
        }

        if (s.contains(" ")){
            int split = s.indexOf(' ');
            String engine = s.substring(0, split).toLowerCase();
            String query = s.substring(split + 1);

            if (!query.contains(" ") && (query.length() == 0 || query.charAt(0) != '-')) {
                if (this._pingAvailable && PingActivity.TARGET_COMMAND_SHORT.startsWith(engine)) {
                    if (StringMatchStrategy.match(context, engine, PingActivity.TARGET_COMMAND_SHORT, true) != -1) {
                        ret.add(new PingCandidateEntry(s.split(" ", 2)[1]));
                    }
                }
                if (this._ping6Available && Ping6Activity.TARGET_COMMAND_SHORT.startsWith(engine)) {
                    if (StringMatchStrategy.match(context, engine, Ping6Activity.TARGET_COMMAND_SHORT, true) != -1) {
                        ret.add(new Ping6CandidateEntry(s.split(" ", 2)[1]));
                    }
                }
            }
        }
        return ret;
    }

    private static List<Pair<String, String>> getFilteredLocalIpAddresses() {
        List<Pair<String, String>> ipv4List = new ArrayList<>();
        List<Pair<String, String>> ipv6List = new ArrayList<>();
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (!intf.isUp() || intf.isLoopback()) continue;
                String name = intf.getName().toLowerCase();
                if (name.startsWith("dummy") || name.startsWith("lo") || name.startsWith("p2p")
                        || name.startsWith("v4-") || name.startsWith("veth") || name.startsWith("sit")) {
                    continue;
                }
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (addr.isLoopbackAddress()) continue;
                    String hostAddr = addr.getHostAddress();
                    if (hostAddr == null) continue;
                    boolean isIPv4 = hostAddr.indexOf(':') < 0;
                    if (isIPv4) {
                        ipv4List.add(new Pair<>("IPv4 " + intf.getName(), hostAddr));
                    } else {
                        int delim = hostAddr.indexOf('%');
                        String cleanAddr = delim < 0 ? hostAddr : hostAddr.substring(0, delim);
                        ipv6List.add(new Pair<>("IPv6 " + intf.getName(), cleanAddr));
                    }
                }
            }
        } catch (Exception ignored) {}
        List<Pair<String, String>> result = new ArrayList<>(ipv4List);
        result.addAll(ipv6List);
        return result;
    }

    private static List<Pair<String, String>> getMacAddresses() {
        List<Pair<String, String>> result = new ArrayList<>();
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (!intf.isUp() || intf.isLoopback()) continue;
                String name = intf.getName().toLowerCase();
                if (name.startsWith("dummy") || name.startsWith("lo") || name.startsWith("p2p")
                        || name.startsWith("v4-") || name.startsWith("veth") || name.startsWith("sit")) {
                    continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                    }
                    result.add(new Pair<>(intf.getName(), sb.toString()));
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    private static List<String> getSystemDnsServers(Context context) {
        List<String> dnsServers = new ArrayList<>();
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork != null) {
                    LinkProperties lp = cm.getLinkProperties(activeNetwork);
                    if (lp != null) {
                        for (InetAddress dns : lp.getDnsServers()) {
                            if (dns.getHostAddress() != null) {
                                dnsServers.add(dns.getHostAddress());
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return dnsServers;
    }

    private static String getWifiSSID(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo info = wifiManager.getConnectionInfo();
                if (info != null && info.getSSID() != null) {
                    String ssid = info.getSSID().replace("\"", "");
                    if (!ssid.equals("<unknown ssid>") && !ssid.isEmpty()) {
                        return ssid;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void fetchDnsAsync(String domain) {
        if (Boolean.TRUE.equals(isFetchingDns.get(domain))) return;
        isFetchingDns.put(domain, true);
        new Thread(() -> {
            try {
                InetAddress[] addrs = InetAddress.getAllByName(domain);
                List<String> ips = new ArrayList<>();
                for (InetAddress addr : addrs) {
                    if (addr.getHostAddress() != null) {
                        ips.add(addr.getHostAddress());
                    }
                }
                dnsCache.put(domain, ips);
            } catch (Exception ignored) {
                dnsCache.put(domain, Collections.emptyList());
            } finally {
                isFetchingDns.remove(domain);
            }
        }).start();
    }

    private static class NetUtilIpCandidateEntry implements CandidateEntry {
        private final List<Pair<String, String>> localIps;
        private final String ssid;
        private final List<Pair<String, String>> macs;

        NetUtilIpCandidateEntry(List<Pair<String, String>> localIps, String ssid, List<Pair<String, String>> macs) {
            this.localIps = localIps;
            this.ssid = ssid;
            this.macs = macs;
        }

        @NonNull
        @Override
        public String getTitle() {
            return "ip";
        }

        @Override
        public View getView(MainActivity mainActivity) {
            LinearLayout layout = new LinearLayout(mainActivity);
            layout.setOrientation(LinearLayout.VERTICAL);
            int paddingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, mainActivity.getResources().getDisplayMetrics());
            layout.setPadding(0, paddingPx, 0, paddingPx);

            TypedValue baseTextColor = new TypedValue();
            mainActivity.getTheme().resolveAttribute(R.attr.andmenuBaseTextColor, baseTextColor, true);

            TextView headerView = new TextView(mainActivity);
            headerView.setText("Network & IP Information");
            headerView.setTypeface(Typeface.DEFAULT_BOLD);
            headerView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            headerView.setTextColor(baseTextColor.data);
            layout.addView(headerView);

            if (!localIps.isEmpty()) {
                for (Pair<String, String> ip : localIps) {
                    TextView tv = new TextView(mainActivity);
                    tv.setText("• " + ip.first + ": " + ip.second);
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                    tv.setTextColor(baseTextColor.data);
                    layout.addView(tv);
                }
            } else {
                TextView tv = new TextView(mainActivity);
                tv.setText("• Local IP: No active connection");
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                tv.setTextColor(baseTextColor.data);
                layout.addView(tv);
            }

            if (ssid != null && !ssid.isEmpty()) {
                TextView tv = new TextView(mainActivity);
                tv.setText("• Wi-Fi SSID: " + ssid);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                tv.setTextColor(baseTextColor.data);
                layout.addView(tv);
            }

            if (!macs.isEmpty()) {
                for (Pair<String, String> mac : macs) {
                    TextView tv = new TextView(mainActivity);
                    tv.setText("• MAC (" + mac.first + "): " + mac.second);
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                    tv.setTextColor(baseTextColor.data);
                    layout.addView(tv);
                }
            }

            return layout;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return null;
        }

        @Override
        public boolean hasEvent() {
            return false;
        }

        @Override
        public boolean hasLongView() {
            return true;
        }

        @Override
        public Drawable getIcon(Context context) {
            return ContextCompat.getDrawable(context, android.R.drawable.ic_menu_info_details);
        }

        @Override
        public boolean isSubItem() {
            return false;
        }

        @Override
        public boolean viewIsRecyclable() {
            return false;
        }
    }

    private static class CopyToClipboardCandidateEntry implements CandidateEntry {
        private final String label;
        private final String value;
        private final boolean executable;

        CopyToClipboardCandidateEntry(String label, String value) {
            this(label, value, true);
        }

        CopyToClipboardCandidateEntry(String label, String value, boolean executable) {
            this.label = label;
            this.value = value;
            this.executable = executable;
        }

        @NonNull
        @Override
        public String getTitle() {
            if (value == null || value.isEmpty()) {
                return label;
            }
            return label + ": " + value;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            if (!executable) return null;
            return activity -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText(label, value);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Copied: " + value, Toast.LENGTH_SHORT).show();
                }
            };
        }

        @Override
        public boolean hasEvent() {
            return executable;
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public Drawable getIcon(Context context) {
            return ContextCompat.getDrawable(context, android.R.drawable.ic_menu_info_details);
        }

        @Override
        public boolean isSubItem() {
            return false;
        }

        @Override
        public boolean viewIsRecyclable() {
            return true;
        }
    }

    private static class PingCandidateEntry implements CandidateEntry {
        private final String _host;

        PingCandidateEntry(String host) {
            this._host = host;
        }

        @Override
        @NonNull
        public String getTitle() {
            return PingActivity.TARGET_COMMAND_SHORT + " " + this._host;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public EventLauncher getEventLauncher(final Context context) {
            return activity -> {
                Intent intent = new Intent(context, PingActivity.class);
                intent.putExtra("host", PingCandidateEntry.this._host);
                activity.startActivityForResult(intent, MainActivity.REQUEST_CODE_FOR_COMING_BACK);
            };
        }

        @Override
        public Drawable getIcon(Context context) {
            return null;
        }

        @Override
        public boolean hasEvent() {
            return !this._host.equals("");
        }

        @Override
        public boolean isSubItem() {
            return false;
        }

        @Override
        public boolean viewIsRecyclable() {
            return true;
        }
    }

    private static class Ping6CandidateEntry implements CandidateEntry {
        private final String _host;

        Ping6CandidateEntry(String host) {
            this._host = host;
        }

        @Override
        @NonNull
        public String getTitle() {
            return Ping6Activity.TARGET_COMMAND_SHORT + " " + this._host;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public EventLauncher getEventLauncher(final Context context) {
            return activity -> {
                Intent intent = new Intent(context, Ping6Activity.class);
                intent.putExtra("host", Ping6CandidateEntry.this._host);
                activity.startActivityForResult(intent, MainActivity.REQUEST_CODE_FOR_COMING_BACK);
            };
        }

        @Override
        public Drawable getIcon(Context context) {
            return null;
        }

        @Override
        public boolean hasEvent() {
            return !this._host.equals("");
        }

        @Override
        public boolean isSubItem() {
            return false;
        }

        @Override
        public boolean viewIsRecyclable() {
            return true;
        }
    }
}
