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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.mozilla.reference.browser.BuildConfig;
import org.mozilla.reference.browser.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class DownloadCoolFileFragment extends Fragment {

    public DownloadCoolFileFragment() {
        // Required empty public constructor
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

        // TODO:
        getActivity().setTitle(getContext().getString(R.string.pref_download_cool_file_title));

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        RecyclerView coolFilesRecyclerView = view.findViewById(R.id.download_cool_file_recycler_view);
        coolFilesRecyclerView.setLayoutManager(linearLayoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(),
                linearLayoutManager.getOrientation());
        coolFilesRecyclerView.addItemDecoration(dividerItemDecoration);

        DownloadCoolFileFragmentRecyclerAdapter downloadCoolFileFragmentRecyclerAdapter =
                new DownloadCoolFileFragmentRecyclerAdapter(getContext());

        coolFilesRecyclerView.setAdapter(downloadCoolFileFragmentRecyclerAdapter);

        DownloadCoolFileViewModel downloadCoolFileViewModel = new ViewModelProvider(this)
                .get(DownloadCoolFileViewModel.class);
        downloadCoolFileViewModel.getAllCoolFiles().observe(getViewLifecycleOwner(), new Observer<HashMap<String, String>>() {
            @Override
            public void onChanged(HashMap<String, String> stringStringHashMap) {
                downloadCoolFileFragmentRecyclerAdapter.submitList(stringStringHashMap);
            }
        });
    }

    static class DownloadCoolFileFragmentRecyclerAdapter extends
            RecyclerView.Adapter<DownloadCoolFileFragmentRecyclerAdapter.ViewHolder> {
        Context context;
        BroadcastReceiver downloadCompleteBroadcastReceiver;

        HashMap<String, String> filesDatabase = new HashMap<>();

        public DownloadCoolFileFragmentRecyclerAdapter(Context context){
            this.context = context;
        }

        public void submitList(HashMap<String, String> filenames) {
            filesDatabase = filenames;
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

            request.setDescription(context.getString(R.string.pref_download_cool_file_download_manager_title));

            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            return downloadManager.enqueue(request);
        }

        @Override
        public void onBindViewHolder(@NonNull DownloadCoolFileFragmentRecyclerAdapter.ViewHolder holder,
                                     int position) {
            Log.d(getClass().getName(), "Pos: " + position);
            String[] filenames = filesDatabase.keySet().toArray(new String[]{});

            final String filename = filenames[position];
            final String fileUrl = filesDatabase.get(filename);

            holder.filename.setText(filename);
            holder.fileUrl.setText(fileUrl);
            DialogInterface.OnClickListener onClickListenerYes = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(context);

                    final EditText editText = new EditText(context);
                    builder1.setView(editText);
                    builder1.setTitle(context.getString(R.string.pref_download_cool_file_filename));
                    builder1.setMessage(context.getString(R.string.pref_download_cool_file_prompt_filename_description));

                    builder1.setPositiveButton(
                            context.getString(R.string.pref_download_cool_file_prompt_download),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String customFilename = editText.getText() != null &&
                                            editText.getText().length() > 0 ?
                                            editText.getText().toString() :
                                            filename;
                                    final long downloadedFileId = downloadFile(fileUrl, customFilename);
                                    registerBroadcastListenerForFile(downloadedFileId);
                                }
                            });

                    builder1.setNegativeButton(context.getString(R.string.pref_download_cool_file_prompt_cancel),
                            new DialogInterface.OnClickListener() { @Override
                            public void onClick(DialogInterface dialog, int which) { }
                    });
                    builder1.show();
                }
            };

            holder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(context.getString(R.string.pref_download_cool_file_title))
                            .setMessage(context.getString(R.string.pref_download_cool_file_prompt))
                            .setPositiveButton(context.getString(R.string.pref_download_cool_file_prompt_yes),
                                    onClickListenerYes)
                            .setNegativeButton(context.getString(R.string.pref_download_cool_file_prompt_no),
                                    new DialogInterface.OnClickListener() {
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
            return filesDatabase.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            TextView filename, fileUrl;
            CardView cardView;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                filename = itemView.findViewById(R.id.cool_files_card_name);
                fileUrl = itemView.findViewById(R.id.cool_files_card_url);
                cardView = itemView.findViewById(R.id.cool_files_card);
            }
        }
    }


    public static class DownloadCoolFileViewModel extends ViewModel {

        /**
         * coolFiles = <cool filename, file url>
         */
        private MutableLiveData<HashMap<String, String>> coolFilesLiveData;
        public MutableLiveData<HashMap<String, String>> getAllCoolFiles() {
            if(coolFilesLiveData == null) {
                coolFilesLiveData = new MutableLiveData<>();
                getCoolFiles();
            }

            return coolFilesLiveData;
        }

        private void getCoolFiles(){
            HashMap<String, String> coolFiles = new HashMap<>();
            coolFiles.put("CENO README.md",
                    "https://gitlab.com/censorship-no/ceno-browser/-/raw/main/README.md");
            coolFiles.put("CENO full description.txt",
                    "https://gitlab.com/censorship-no/ceno-browser/-/raw/main/fastlane/metadata/android/en-US/full_description.txt");

            coolFilesLiveData.setValue(coolFiles);
        }
    }
}