package io.virtualapp.shadowsocks.core;



import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import io.virtualapp.shadowsocks.tunnel.Config;
import io.virtualapp.shadowsocks.tunnel.RawTunnel;
import io.virtualapp.shadowsocks.tunnel.Tunnel;
import io.virtualapp.shadowsocks.tunnel.httpconnect.HttpConnectConfig;
import io.virtualapp.shadowsocks.tunnel.httpconnect.HttpConnectTunnel;
import io.virtualapp.shadowsocks.tunnel.shadowsocks.ShadowsocksConfig;
import io.virtualapp.shadowsocks.tunnel.shadowsocks.ShadowsocksTunnel;

public class TunnelFactory {

    public static Tunnel wrap(SocketChannel channel, Selector selector) {
        return new RawTunnel(channel, selector);
    }

    public static Tunnel createTunnelByConfig(InetSocketAddress destAddress, Selector selector) throws Exception {
        if (destAddress.isUnresolved()) {
            Config config = ProxyConfig.Instance.getDefaultTunnelConfig(destAddress);
            if (config instanceof HttpConnectConfig) {
                return new HttpConnectTunnel((HttpConnectConfig) config, selector);
            } else if (config instanceof ShadowsocksConfig) {
                return new ShadowsocksTunnel((ShadowsocksConfig) config, selector);
            }
            throw new Exception("The config is unknow.");
        } else {
            return new RawTunnel(destAddress, selector);
        }
    }

}
