package ppex.androidcomponent.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ppex.client.R;
import ppex.androidcomponent.entity.Files;

public class FilesAdapter extends BaseAdapter {

    private List<Files> files = new ArrayList<>();
    private Context context;

    public FilesAdapter(List<Files> files, Context context) {
        this.files = files;
        this.context = context;
    }

    @Override
    public int getCount() {
        return files == null ? 0 : files.size();
    }

    @Override
    public Object getItem(int position) {
        return null == files ? null : files.get(position);
    }

    @Override
    public long getItemId(int position) {
        return null == files ? 0:files.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder = null;
        if (convertView == null) {
            holder = new Holder();
            convertView = View.inflate(context, R.layout.item_files,null);
            holder.iv_showtype = convertView.findViewById(R.id.iv_item_files_showtype);
            holder.tv_showname = convertView.findViewById(R.id.tv_item_files_showname);
            convertView.setTag(holder);
        }else{
            holder = (Holder) convertView.getTag();
        }
        Files files = (Files) getItem(position);
        if (files.isDirectory()){

        }
        holder.tv_showname.setText(files.getName());

        return convertView;
    }

    public void setFiles(List<Files> files) {
        this.files = files;
    }

    public final class Holder{
        public ImageView iv_showtype;
        public TextView tv_showname;
    }
}
