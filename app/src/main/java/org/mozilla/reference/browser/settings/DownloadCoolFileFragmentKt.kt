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
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.reference.browser.R


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
class DownloadCoolFileFragmentKt : Fragment() {

    private val downloadCoolFileViewModel : DownloadCoolFileViewModel by viewModels()

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

        downloadCoolFileViewModel.getAllCoolFiles().observe(viewLifecycleOwner,
                Observer { downloadableCoolFile : Array<DownloadableCoolFile> ->
                    run {
                        downloadCoolFileFragmentRecyclerAdapter?.submitList(downloadableCoolFile)
                    }
                }
        )

        val addNewCoolFileBtn : Button = view.findViewById(R.id.download_cool_file_add_new_btn);
        addNewCoolFileBtn.setOnClickListener(onClickListenerAddCoolFile);
    }

    class DownloadableCoolFile {
        var filename : String = ""
        var fileUrl : String = ""
        var fileType : String = ""

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false

            val otherFile = other as DownloadableCoolFile

            return filename == otherFile.filename &&
                    fileUrl == otherFile.fileUrl &&
                    fileType == otherFile.fileType
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

        private var mDiffer : AsyncListDiffer<DownloadableCoolFile> =
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

        public fun refreshNewCoolFiles( downloadableCoolFile : DownloadableCoolFile) {
            var coolFiles : Array<DownloadableCoolFile>? = coolFilesLiveData.value;
            coolFiles = coolFiles?.plus(downloadableCoolFile)

            coolFilesLiveData.value = coolFiles!!;
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

    private val onClickListenerAddCoolFile : View.OnClickListener = View.OnClickListener {
        val builder: AlertDialog.Builder? = context?.let { it1 -> AlertDialog.Builder(it1) };

        builder?.setTitle(getString(R.string.pref_download_cool_file_new_btn));
        builder?.setMessage(getString(R.string.pref_download_cool_file_add_new_file_url));

        val inflater : LayoutInflater = layoutInflater;
        val newCoolFileViewLayout : View = inflater.inflate(R.layout.view_add_new_cool_file, null);

        builder?.setView(newCoolFileViewLayout);

        val coolFileUrlEditText : EditText = newCoolFileViewLayout.findViewById(R.id.add_new_cool_file_url_input);
        val coolFileNameEditText : EditText = newCoolFileViewLayout.findViewById(R.id.add_new_cool_file_name);
        val radioGroup : RadioGroup = newCoolFileViewLayout.findViewById(R.id.cool_file_file_type_radio_group);

        builder?.setPositiveButton(getString(R.string.pref_download_cool_file_add_new_file),
                DialogInterface.OnClickListener { _: DialogInterface, _: Int ->
                    var coolFileType = "text/plain";

                    if(radioGroup.checkedRadioButtonId == R.id.cool_file_pdf_radio_btn) {
                        coolFileType = "application/pdf";
                    }

                    val coolFilename = coolFileNameEditText.text.toString();
                    val coolFileUrl = coolFileUrlEditText.text.toString();

                    val downloadableCoolFile = DownloadableCoolFile();
                    downloadableCoolFile.filename = coolFilename;
                    downloadableCoolFile.fileType = coolFileType;
                    downloadableCoolFile.fileUrl = coolFileUrl;

                    downloadCoolFileViewModel.refreshNewCoolFiles(downloadableCoolFile);
                })?.setNegativeButton(getString(R.string.pref_download_cool_file_prompt_cancel),
                DialogInterface.OnClickListener { _:DialogInterface, _: Int -> {}});

        builder?.show();
    };
}