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
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.mozilla.reference.browser.BuildConfig;
import org.mozilla.reference.browser.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * - [Decisions for each method used.]
 *
 * - A fragment is used here to maintain the uniformity of the codebase.
 * - A RecyclerView is used to build a list of default downloadable cool files.
 * - A custom view at layout.view_cool_files_item.xml is added to display the items in the RecyclerView.
 * - A ViewModel is used with the RecyclerView to ease separating and accessing the data sources/layer.
 * - A class called DownloadableCoolFile is created so that we can different between objects in the
 *      RecyclerView using AsyncListDiffer. This is a background method of computing the differences
 *      between objects for a RecyclerView.
 * - A button is added so that the user can download custom filetypes apart from the defaults. When
 *      new cool files are added they are submitted to the RecyclerView to demonstrate how the ViewModel
 *      and RecyclerView work as data and representation layers respectively.
 * - A custom view at layout.view_add_new_cool_file is used to create a customized alert box that can
 *      have the required inputs types.
 *
 * - [Important note]
 * --> The data is not persistent as no persistent storage is used. Everything is populated and used
 *      from default variable values.
 * --> Data verification is at the minimal; currently does not check if downloaded filetype matches
 *      the registered filetype.
 */
public class DownloadCoolFileFragment extends Fragment {

    DownloadCoolFileFragmentRecyclerAdapter downloadCoolFileFragmentRecyclerAdapter;
    DownloadCoolFileViewModel downloadCoolFileViewModel;

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

        getActivity().setTitle(getContext().getString(R.string.pref_download_cool_file_title));

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        RecyclerView coolFilesRecyclerView = view.findViewById(R.id.download_cool_file_recycler_view);
        coolFilesRecyclerView.setLayoutManager(linearLayoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(),
                linearLayoutManager.getOrientation());
        coolFilesRecyclerView.addItemDecoration(dividerItemDecoration);

        downloadCoolFileFragmentRecyclerAdapter =
                new DownloadCoolFileFragmentRecyclerAdapter(getContext());

        coolFilesRecyclerView.setAdapter(downloadCoolFileFragmentRecyclerAdapter);

        downloadCoolFileViewModel = new ViewModelProvider(this)
                .get(DownloadCoolFileViewModel.class);
        downloadCoolFileViewModel.getAllCoolFiles().observe(getViewLifecycleOwner(), new Observer<List<DownloadableCoolFile>>() {
            @Override
            public void onChanged(List<DownloadableCoolFile> downloadableCoolFileList) {
                downloadCoolFileFragmentRecyclerAdapter.submitList(downloadableCoolFileList);
            }
        });

        Button addNewCoolFileBtn = view.findViewById(R.id.download_cool_file_add_new_btn);
        addNewCoolFileBtn.setOnClickListener(onClickListenerAddCoolFile);
    }

    private static class DownloadableCoolFile {
        private String filename = "";
        private String fileUrl = "";
        private String fileType = "";

        @Override
        public boolean equals(@Nullable Object obj) {
            if(obj != null) {
                DownloadableCoolFile downloadableCoolFile = (DownloadableCoolFile) obj;
                return downloadableCoolFile.filename.equals(this.filename) &&
                        downloadableCoolFile.fileUrl.equals(this.fileUrl) &&
                        downloadableCoolFile.fileType.equals(this.fileType);
            }
            return false;
        }

        public static final DiffUtil.ItemCallback<DownloadableCoolFile> DIFF_CALLBACK =
                new DiffUtil.ItemCallback<DownloadableCoolFile>() {
            @Override
            public boolean areItemsTheSame(@NonNull DownloadableCoolFile oldItem, @NonNull DownloadableCoolFile newItem) {
                return oldItem.filename.equals(newItem.filename);
            }

            @Override
            public boolean areContentsTheSame(@NonNull DownloadableCoolFile oldItem, @NonNull DownloadableCoolFile newItem) {
                return oldItem.equals(newItem);
            }
        };
    }


    static class DownloadCoolFileFragmentRecyclerAdapter extends
            RecyclerView.Adapter<DownloadCoolFileFragmentRecyclerAdapter.ViewHolder> {

        private final AsyncListDiffer<DownloadableCoolFile> mDiffer = new AsyncListDiffer(this, DownloadableCoolFile.DIFF_CALLBACK);
        Context context;
        BroadcastReceiver downloadCompleteBroadcastReceiver;

        public DownloadCoolFileFragmentRecyclerAdapter(Context context){
            this.context = context;
        }

        public void submitList(List<DownloadableCoolFile> filenames) {
            mDiffer.submitList(filenames);
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
            DownloadableCoolFile downloadableCoolFile = mDiffer.getCurrentList().get(position);

            final String filename = downloadableCoolFile.filename;
            final String fileUrl = downloadableCoolFile.fileUrl;
            final String fileType = downloadableCoolFile.fileType;

            holder.filename.setText(filename);
            holder.fileUrl.setText(fileUrl);
            holder.fileType.setText(fileType);
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
                                    String customFilename = editText.getText() != null && editText.getText().length() > 0 ?
                                            editText.getText().toString() :
                                            filename;
                                    final long downloadedFileId = downloadFile(fileUrl, customFilename);
                                    registerBroadcastListenerForFile(downloadedFileId, fileType);

                                    Toast toast = Toast.makeText(context,
                                            context.getString(R.string.pref_download_cool_file_add_download_queued),
                                            Toast.LENGTH_SHORT);
                                    toast.show();
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

        public void registerBroadcastListenerForFile(final long downloadFileId, final String fileType) {
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
                        openIntent.setDataAndType(downloadUri, fileType);
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
            return mDiffer.getCurrentList().size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            TextView filename, fileUrl, fileType;
            CardView cardView;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                filename = itemView.findViewById(R.id.cool_files_card_name);
                fileUrl = itemView.findViewById(R.id.cool_files_card_url);
                fileType = itemView.findViewById(R.id.cool_files_file_type);
                cardView = itemView.findViewById(R.id.cool_files_card);
            }
        }
    }


    public static class DownloadCoolFileViewModel extends ViewModel {

        private MutableLiveData<List<DownloadableCoolFile>> coolFilesLiveData;
        public MutableLiveData<List<DownloadableCoolFile>> getAllCoolFiles() {
            if(coolFilesLiveData == null) {
                coolFilesLiveData = new MutableLiveData<>();
                getCoolFiles();
            }

            return coolFilesLiveData;
        }

        public void refreshNewCoolFiles(DownloadableCoolFile downloadableCoolFile) {
            List<DownloadableCoolFile> coolFiles = coolFilesLiveData.getValue();
            coolFiles.add(downloadableCoolFile);

            coolFilesLiveData.setValue(coolFiles);
        }

        private void getCoolFiles(){
            List<DownloadableCoolFile> downloadableCoolFileList = new ArrayList<>();

            DownloadableCoolFile downloadableCoolFile = new DownloadableCoolFile();
            downloadableCoolFile.filename = "CENO README.md";
            downloadableCoolFile.fileUrl = "https://gitlab.com/censorship-no/ceno-browser/-/raw/main/README.md";
            downloadableCoolFile.fileType = "text/plain";
            downloadableCoolFileList.add(downloadableCoolFile);

            downloadableCoolFile = new DownloadableCoolFile();
            downloadableCoolFile.filename = "CENO full description.txt";
            downloadableCoolFile.fileUrl = "https://gitlab.com/censorship-no/ceno-browser/-/raw/main/fastlane/metadata/android/en-US/full_description.txt";
            downloadableCoolFile.fileType = "text/plain";
            downloadableCoolFileList.add(downloadableCoolFile);

            downloadableCoolFile = new DownloadableCoolFile();
            downloadableCoolFile.filename = "CENO fact sheet.pdf";
            downloadableCoolFile.fileUrl = "https://censorship.no/img/factsheet.pdf";
            downloadableCoolFile.fileType = "application/pdf";
            downloadableCoolFileList.add(downloadableCoolFile);

            coolFilesLiveData.setValue(downloadableCoolFileList);
        }
    }

    private final View.OnClickListener onClickListenerAddCoolFile = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

            builder.setTitle(getString(R.string.pref_download_cool_file_new_btn));

            builder.setMessage(getString(R.string.pref_download_cool_file_add_new_file_url));

            LayoutInflater inflater = getLayoutInflater();
            View newCoolFileViewLayout = inflater.inflate(R.layout.view_add_new_cool_file, null);

            builder.setView(newCoolFileViewLayout);

            EditText coolFileUrlEditText = newCoolFileViewLayout.findViewById(R.id.add_new_cool_file_url_input);
            EditText coolFileNameEditText = newCoolFileViewLayout.findViewById(R.id.add_new_cool_file_name);
            RadioGroup radioGroup = newCoolFileViewLayout.findViewById(R.id.cool_file_file_type_radio_group);

            builder.setPositiveButton(getString(R.string.pref_download_cool_file_add_new_file),
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String coolFileType = "text/plain";

                    if(radioGroup.getCheckedRadioButtonId() == R.id.cool_file_pdf_radio_btn) {
                        coolFileType = "application/pdf";
                    }

                    String coolFilename = coolFileNameEditText.getText().toString();
                    String coolFileUrl = coolFileUrlEditText.getText().toString();

                    DownloadableCoolFile downloadableCoolFile = new DownloadableCoolFile();
                    downloadableCoolFile.filename = coolFilename;
                    downloadableCoolFile.fileType = coolFileType;
                    downloadableCoolFile.fileUrl = coolFileUrl;

                    downloadCoolFileViewModel.refreshNewCoolFiles(downloadableCoolFile);
                }
            }).setNegativeButton(getString(R.string.pref_download_cool_file_prompt_cancel),
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            builder.show();
        }
    };
}