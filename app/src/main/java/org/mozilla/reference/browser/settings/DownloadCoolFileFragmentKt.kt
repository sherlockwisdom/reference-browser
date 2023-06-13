package org.mozilla.reference.browser.settings

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.mozilla.reference.browser.R

class DownloadCoolFileFragmentKt : Fragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_download_cool_file, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment DownloadCoolFileFragmentKt.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
                DownloadCoolFileFragmentKt().apply {
                    arguments = Bundle().apply { }
                }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.title = context?.getString(R.string.pref_download_cool_file_title)

        val linearLayoutManager : LinearLayoutManager = LinearLayoutManager(context);
        val recyclerView : RecyclerView = view.findViewById(R.id.download_cool_file_recycler_view)

        recyclerView.layoutManager = linearLayoutManager

        val dividerItemDecoration : DividerItemDecoration = DividerItemDecoration(context,
                linearLayoutManager.orientation)
        recyclerView.addItemDecoration(dividerItemDecoration)

        val downloadCoolFileFragmentRecyclerAdapter = context?.let { DownloadCoolFileFragmentRecyclerAdapter(it) };
        recyclerView.adapter = downloadCoolFileFragmentRecyclerAdapter

        val downloadCoolFileViewModel : DownloadCoolFileViewModel by viewModels()
        downloadCoolFileViewModel.getAllCoolFiles().observe(viewLifecycleOwner,
                Observer { downloadableCoolFile : Array<DownloadableCoolFile> ->
                    run {
                        downloadCoolFileFragmentRecyclerAdapter?.submitList(downloadableCoolFile)
                    }
                }
        )
    }

    class DownloadableCoolFile {
        var filename : String = String()
        var fileUrl : String = String()
        var fileType : String = String()

        override fun equals(other: Any?): Boolean {
            when(other) {
                is DownloadableCoolFile -> return other.fileType == this.fileType &&
                        other.filename == this.filename &&
                        other.fileUrl == this.fileUrl
            }
            return false;
        }

        override fun hashCode(): Int {
            var result = filename.hashCode()
            result = 31 * result + fileUrl.hashCode()
            result = 31 * result + fileType.hashCode()
            return result
        }

        class DIFF_CALLBACK : DiffUtil.ItemCallback<DownloadableCoolFile>() {
            override fun areItemsTheSame(oldItem: DownloadableCoolFile, newItem: DownloadableCoolFile): Boolean {
                return oldItem.filename == newItem.filename
            }

            override fun areContentsTheSame(oldItem: DownloadableCoolFile, newItem: DownloadableCoolFile): Boolean {
                return oldItem == newItem
            }
        }
    }

    class DownloadCoolFileFragmentRecyclerAdapter(val context: Context) :
            RecyclerView.Adapter<DownloadCoolFileFragmentRecyclerAdapter.ViewHolder>() {

        var mDiffer : AsyncListDiffer<DownloadableCoolFile> =
                AsyncListDiffer(this, DownloadableCoolFile.DIFF_CALLBACK());

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var filename : TextView = itemView.findViewById(R.id.cool_files_card_name);
            var fileUrl : TextView = itemView.findViewById(R.id.cool_files_card_url);
            var fileType : TextView = itemView.findViewById(R.id.cool_files_file_type);

            var cardView : CardView = itemView.findViewById(R.id.cool_files_card);
        }

        fun submitList(downloadableCoolFileList : Array<DownloadableCoolFile> ) {
            mDiffer.submitList(downloadableCoolFileList.toList())
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater: LayoutInflater = LayoutInflater.from(context);
            val view : View = inflater.inflate(R.layout.view_cool_files_item, parent, false);

            return ViewHolder(view);
        }

        override fun getItemCount(): Int {
            return mDiffer.currentList.size
        }

        fun downloadFile(fileUrl : String, fileName : String) : Long {
            val request : DownloadManager.Request = DownloadManager.Request(Uri.parse(fileUrl));
            request.setTitle(fileName);

            request.setDescription(context.getString(R.string.pref_download_cool_file_download_manager_title));

            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            val downloadManager : DownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            return downloadManager.enqueue(request);
        }

        fun registerBroadcastListenerForFile(downloadFileId : Long, fileType : String) {
            // Register a broadcast receiver to listen for the download completion
            class DownloadCompleteBroadcastReceiver : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val completedDownloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,
                            -1);

                    if (downloadFileId == completedDownloadId) {
                        context?.unregisterReceiver(this);

                        val downloadManager : DownloadManager =
                                context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                        val downloadUri : Uri = downloadManager.getUriForDownloadedFile(downloadFileId);

                        // Open the file with the browser
                        val openIntent = Intent(Intent.ACTION_VIEW);
                        openIntent.setDataAndType(downloadUri, fileType);
                        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        when(openIntent.resolveActivity(context.packageManager)) {
                            is ComponentName -> context.startActivity(openIntent);
                        }
                    }
                }
            }

            context.registerReceiver(DownloadCompleteBroadcastReceiver(),
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val downloadableCoolFile : DownloadableCoolFile = mDiffer.currentList[position]

            val filename = downloadableCoolFile.filename
            val fileUrl = downloadableCoolFile.fileUrl
            val fileType = downloadableCoolFile.fileType

            holder.filename.text = filename
            holder.fileUrl.text = fileUrl
            holder.fileType.text = fileType

            class OnClickListenerYes : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    val builder1 : AlertDialog.Builder = AlertDialog.Builder(context);

                    val editText : EditText = EditText(context);
                    builder1.setView(editText);
                    builder1.setTitle(context.getString(R.string.pref_download_cool_file_filename));
                    builder1.setMessage(context.getString(R.string.pref_download_cool_file_prompt_filename_description));

                    builder1.setPositiveButton(
                            context.getString(R.string.pref_download_cool_file_prompt_download),
                            DialogInterface.OnClickListener { dialog, which ->
                                run {
                                    val customFilename: String = editText.text.toString().ifEmpty { filename }
                                    val downloadedFileId : Long  = downloadFile(fileUrl, customFilename);
                                    registerBroadcastListenerForFile(downloadedFileId, fileType);

                                    val toast : Toast = Toast.makeText (context,
                                            context.getString(R.string.pref_download_cool_file_add_download_queued),
                                            Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            })

                    builder1.setNegativeButton(context.getString(R.string.pref_download_cool_file_prompt_cancel),
                            DialogInterface.OnClickListener { _, _ ->  })
                    builder1.show();
                }
            };

            holder.cardView.setOnClickListener {
                val builder: AlertDialog.Builder = AlertDialog.Builder(context);

                builder.setTitle(context.getString(R.string.pref_download_cool_file_title))
                        .setMessage(context.getString(R.string.pref_download_cool_file_prompt))
                        .setPositiveButton(context.getString(R.string.pref_download_cool_file_prompt_yes),
                                OnClickListenerYes())
                        .setNegativeButton(context.getString(R.string.pref_download_cool_file_prompt_no),
                                DialogInterface.OnClickListener { _, _ -> })

                val dialog : AlertDialog = builder.create();
                dialog.show();
            };
        }
    }


    class DownloadCoolFileViewModel : ViewModel() {

        private val coolFilesLiveData : MutableLiveData<Array<DownloadableCoolFile>> = MutableLiveData();

        public fun getAllCoolFiles() : MutableLiveData<Array<DownloadableCoolFile>> {
            getCoolFiles()
            return coolFilesLiveData
        }

        private fun getCoolFiles(){
            val downloadableCoolFileCenoReadMe = DownloadableCoolFile();
            downloadableCoolFileCenoReadMe.filename = "CENO README.md";
            downloadableCoolFileCenoReadMe.fileUrl = "https://gitlab.com/censorship-no/ceno-browser/-/raw/main/README.md";
            downloadableCoolFileCenoReadMe.fileType = "text/plain";

            val downloadableCoolFileCenoDescription = DownloadableCoolFile();
            downloadableCoolFileCenoDescription.filename = "CENO full description.txt";
            downloadableCoolFileCenoDescription.fileUrl = "https://gitlab.com/censorship-no/ceno-browser/-/raw/main/fastlane/metadata/android/en-US/full_description.txt";
            downloadableCoolFileCenoDescription.fileType = "text/plain";

            val downloadableCoolFileCenoFactSheet = DownloadableCoolFile();
            downloadableCoolFileCenoFactSheet.filename = "CENO fact sheet.pdf";
            downloadableCoolFileCenoFactSheet.fileUrl = "https://censorship.no/img/factsheet.pdf";
            downloadableCoolFileCenoFactSheet.fileType = "application/pdf";

            val downloadableCoolFileList : Array<DownloadableCoolFile> = arrayOf(
                    downloadableCoolFileCenoReadMe,
                    downloadableCoolFileCenoDescription,
                    downloadableCoolFileCenoFactSheet)

            coolFilesLiveData.value = downloadableCoolFileList;
        }
    }
}