package org.mozilla.reference.browser.settings;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.mozilla.reference.browser.BuildConfig;
import org.mozilla.reference.browser.R;

import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DownloadCoolFileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DownloadCoolFileFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    public DownloadCoolFileFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment DownloadCoolFileFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DownloadCoolFileFragment newInstance(String param1, String param2) {
        DownloadCoolFileFragment fragment = new DownloadCoolFileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_download_cool_file, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        RecyclerView coolFilesRecyclerView = view.findViewById(R.id.download_cool_file_recycler_view);
        coolFilesRecyclerView.setLayoutManager(linearLayoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(),
                linearLayoutManager.getOrientation());
        coolFilesRecyclerView.addItemDecoration(dividerItemDecoration);

        DownloadCoolFileFragmentRecyclerAdapter downloadCoolFileFragmentRecyclerAdapter =
                new DownloadCoolFileFragmentRecyclerAdapter(getContext());

        coolFilesRecyclerView.setAdapter(downloadCoolFileFragmentRecyclerAdapter);

    }

    static class DownloadCoolFileFragmentRecyclerAdapter extends
            RecyclerView.Adapter<DownloadCoolFileFragmentRecyclerAdapter.ViewHolder> {
        Context context;
        BroadcastReceiver downloadCompleteBroadcastReceiver;

        ArrayList<String> filenames = new ArrayList<>();

        public DownloadCoolFileFragmentRecyclerAdapter(Context context){
            this.context = context;
            filenames.add("https://gitlab.com/censorship-no/ceno-browser/-/raw/main/README.md");
        }

        @NonNull
        @Override
        public DownloadCoolFileFragmentRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.view_cool_files_item, parent, false);
            return new ViewHolder(view);
        }

        private long downloadFile(String fileUrl, String fileName) {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
            request.setTitle(fileName);

            // TODO:
            request.setDescription("Downloading file...");

            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            return downloadManager.enqueue(request);
        }

        @Override
        public void onBindViewHolder(@NonNull DownloadCoolFileFragmentRecyclerAdapter.ViewHolder holder,
                                     int position) {
            holder.filename.setText(filenames.get(position));

            final String fileUrl = filenames.get(position);
            holder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Download file")
                            .setMessage("Are you sure?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String filename = "sample file.txt";
                                    final long downloadedFileId = downloadFile(fileUrl, filename);
                                    registerBroadcastListenerForFile(downloadedFileId);
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
        }

        public void registerBroadcastListenerForFile(final long downloadFileId) {
            // Register a broadcast receiver to listen for the download completion
            downloadCompleteBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    long completedDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,
                            -1);

                    if(BuildConfig.DEBUG)
                        Log.d(getClass().getName(), "New download finished broadcast..." +
                                completedDownloadId + ":" + downloadFileId);

                    if (downloadFileId == completedDownloadId) {
                        context.unregisterReceiver(this);

                        DownloadManager downloadManager =
                                (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                        Uri downloadUri = downloadManager.getUriForDownloadedFile(downloadFileId);

                        // Open the file with the browser
                        Intent openIntent = new Intent(Intent.ACTION_VIEW);
                        openIntent.setDataAndType(downloadUri, "text/plain");
                        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        if (openIntent.resolveActivity(context.getPackageManager()) != null) {
                            context.startActivity(openIntent);
                        }
                    }
                }
            };

            context.registerReceiver(downloadCompleteBroadcastReceiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }

        @Override
        public int getItemCount() {
            return filenames.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            TextView filename;
            CardView cardView;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                filename = itemView.findViewById(R.id.cool_files_card_name);
                cardView = itemView.findViewById(R.id.cool_files_card);
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}