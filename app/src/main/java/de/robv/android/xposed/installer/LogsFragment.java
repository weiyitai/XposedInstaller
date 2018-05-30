package de.robv.android.xposed.installer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static de.robv.android.xposed.installer.XposedApp.WRITE_EXTERNAL_PERMISSION;

public class LogsFragment extends Fragment {

    private File mFileErrorLog = new File(XposedApp.BASE_DIR + "log/error.log");
    //    private File mFileErrorLog = new File(Environment.getExternalStorageDirectory() + "/1/", "error.log");
    private File mFileErrorLogOld = new File(
            XposedApp.BASE_DIR + "log/error.log.old");
    private HorizontalScrollView mHSVLog;
    private MenuItem mClickedMenuItem = null;
    private ProgressDialog mProgressDialog;
    private LogsAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private boolean mScroll2Bottom;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tab_logs, container, false);
///        mHSVLog = (HorizontalScrollView) v.findViewById(R.id.hsvLog);

        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.rv_log);
        mAdapter = new LogsAdapter();
        mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(mAdapter);

        reloadErrorLog(true);

        View reload = v.findViewById(R.id.reload);
        View scrollTop = v.findViewById(R.id.scroll_top);
        View scrollDown = v.findViewById(R.id.scroll_down);
        reload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reloadErrorLog(false);
            }
        });
        scrollTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollTop();
            }
        });
        scrollDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollDown();
            }
        });

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_logs, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mClickedMenuItem = item;
        switch (item.getItemId()) {
            case R.id.menu_scroll_top:
                scrollTop();
                break;
            case R.id.menu_scroll_down:
                scrollDown();
                break;
            case R.id.menu_refresh:
                reloadErrorLog(false);
                return true;
            case R.id.menu_send:
                try {
                    send();
                } catch (NullPointerException ignored) {
                }
                return true;
            case R.id.menu_save:
                save();
                return true;
            case R.id.menu_clear:
                clear();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scrollTop() {
        mLayoutManager.scrollToPosition(0);
    }

    private void scrollDown() {
        int itemCount = mAdapter.getItemCount();
        Log.d("LogsFragment", "itemCount:" + itemCount);
        mLayoutManager.scrollToPositionWithOffset(itemCount - 1, 0);
    }

    private void reloadErrorLog(boolean scroll) {
        mScroll2Bottom = scroll;
        long millis2 = System.currentTimeMillis();
        new LogsReader().execute(mFileErrorLog);
        final long millis3 = System.currentTimeMillis();
        Log.d("LogsFragment", "reload>take:" + (millis3 - millis2));
    }

    private void clear() {
        try {
            new FileOutputStream(mFileErrorLog).close();
            mFileErrorLogOld.delete();
            Toast.makeText(getActivity(), R.string.logs_cleared,
                    Toast.LENGTH_SHORT).show();
            reloadErrorLog(true);
        } catch (IOException e) {
            Toast.makeText(getActivity(), getResources().getString(R.string.logs_clear_failed) + "n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void send() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(save()));
        sendIntent.setType("application/html");
        startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.menuSend)));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions,
                grantResults);
        if (requestCode == WRITE_EXTERNAL_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mClickedMenuItem != null) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onOptionsItemSelected(mClickedMenuItem);
                        }
                    }, 500);
                }
            } else {
                Toast.makeText(getActivity(), R.string.permissionNotGranted, Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private File save() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_PERMISSION);
            return null;
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(getActivity(), R.string.sdcard_not_writable, Toast.LENGTH_LONG).show();
            return null;
        }
        File dir = getActivity().getExternalFilesDir(null);

        if (!dir.exists()) {
            dir.mkdir();
        }

        String format = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File targetFile = new File(dir, "xposed_" + format + ".log");

        try {
            FileInputStream in = new FileInputStream(mFileErrorLog);
            FileOutputStream out = new FileOutputStream(targetFile);
            byte[] buffer = new byte[1024 * 8];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();

            Toast.makeText(getActivity(), targetFile.toString(),
                    Toast.LENGTH_LONG).show();
            return targetFile;
        } catch (IOException e) {
            Toast.makeText(getActivity(), getResources().getString(R.string.logs_save_failed) + "\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private class LogsReader extends AsyncTask<File, Integer, List<String>> {

        private static final int MAX_LOG_SIZE = 1000 * 1024; // 1000 KB

        private long skipLargeFile(BufferedReader is, long length) throws IOException {
            if (length < MAX_LOG_SIZE) {
                return 0;
            }

            long skipped = length - MAX_LOG_SIZE;
            long yetToSkip = skipped;
            do {
                yetToSkip -= is.skip(yetToSkip);
            } while (yetToSkip > 0);

            int c;
            do {
                c = is.read();
                if (c == -1) {
                    break;
                }
                skipped++;
            } while (c != '\n');

            return skipped;

        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.show();
//            mProgressDialog = new MaterialDialog.Builder(getActivity()).content(R.string.loading).progress(true, 0).show();
        }

        @Override
        protected List<String> doInBackground(File... log) {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);
            long millis = System.currentTimeMillis();
            StringBuilder sb = new StringBuilder(1024 * 10);
            List<String> list = new ArrayList<>();
            try {
                File logfile = log[0];
                BufferedReader br;
                br = new BufferedReader(new FileReader(logfile));
                long skipped = skipLargeFile(br, logfile.length());
                if (skipped > 0) {
                    sb.append("-----------------\n");
                    sb.append("Log too long");
                    sb.append("\n-----------------\n\n");
                }

                char[] temp = new char[1024 * 8];
                int read, c = 0;
                while ((read = br.read(temp)) > 0) {
                    sb.append(temp, 0, read);
                    sb.append(br.readLine());
                    list.add(sb.toString());
                    sb.setLength(0);
                }
                br.close();
                long millis1 = System.currentTimeMillis();
                Log.d("LogsReader", "doInBackground>take:" + (millis1 - millis));
            } catch (IOException e) {
                sb.append("Cannot read log");
                sb.append(e.getMessage());
            }
            return list;
        }

        @Override
        protected void onPostExecute(List<String> list) {
            if (list.isEmpty()) {
                list.add(getString(R.string.log_is_empty));
            }
            long millis = System.currentTimeMillis();
            mAdapter.setData(list);
            long millis1 = System.currentTimeMillis();
            Log.d("LogsReader", "setData>take:" + (millis1 - millis));
            if (mScroll2Bottom) {
                scrollDown();
            }
            mProgressDialog.dismiss();
        }

    }
}