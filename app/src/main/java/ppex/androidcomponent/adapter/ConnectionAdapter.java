package ppex.androidcomponent.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import ppex.client.R;
import ppex.proto.entity.Connection;
import ppex.utils.NatTypeUtil;

public class ConnectionAdapter extends BaseAdapter {

    private List<Connection> connectios;
    private Context context;

    public ConnectionAdapter(List<Connection> connections, Context context) {
        this.connectios = connections;
        this.context = context;
    }

    public void setConnectios(List<Connection> connectios) {
        this.connectios = connectios;
    }

    @Override
    public int getCount() {
        return connectios == null ? 0 : connectios.size();
    }

    @Override
    public Object getItem(int position) {
        return connectios == null ? null : connectios.get(position);
    }

    @Override
    public long getItemId(int position) {
        return connectios == null ? 0 : connectios.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder = null;
        if (convertView == null) {
            holder = new Holder();
            convertView = View.inflate(context, R.layout.item_connection,null);
            holder.tv_showmacaddress = convertView.findViewById(R.id.tv_item_showmacaddress);
            holder.tv_shownattype = convertView.findViewById(R.id.tv_item_shownattype);
            holder.tv_showpeername = convertView.findViewById(R.id.tv_item_showpeername);
            holder.tv_shownetsocketaddress = convertView.findViewById(R.id.tv_item_shownetsocketadress);
            convertView.setTag(holder);
        }else{
            holder = (Holder) convertView.getTag();
        }

        Connection connection = (Connection) getItem(position);
        holder.tv_shownetsocketaddress.setText(connection.getAddress() == null ? "" : connection.getAddress().toString());
        holder.tv_showpeername.setText(connection.getPeerName());
        holder.tv_showmacaddress.setText(connection.getMacAddress());
        holder.tv_shownattype.setText(NatTypeUtil.getNatStrByValue(connection.getNatType()));

        return convertView;
    }

    public final class Holder{
        public TextView tv_showmacaddress,tv_shownetsocketaddress,tv_shownattype,tv_showpeername;
    }
}
