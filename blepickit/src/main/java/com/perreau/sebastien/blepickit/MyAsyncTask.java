package com.perreau.sebastien.blepickit;

import android.os.AsyncTask;

public class MyAsyncTask extends AsyncTask<Void, Integer, Long>
{

    public interface Listener
    {
        void onPreExecute();
        Long doInBackground(MyAsyncTask task);
        void onPostExecute(Long result);
        void onProgressUpdate(Integer... values);
    }
    private final Listener callback;


    public MyAsyncTask(Listener mCallBack)
    {
        this.callback = mCallBack;
        this.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    // Method executed in the MainThread (also called UI Thread)
    @Override
    protected void onPreExecute()
    {
        super.onPreExecute();
        this.callback.onPreExecute();
    }

    // Method executed in a dedicated Thread (not in the MainThread)
    @Override
    protected Long doInBackground(Void... voids)
    {
        return this.callback.doInBackground(this);
    }

    // Method executed in the MainThread (also called UI Thread)
    @Override
    protected void onPostExecute(Long result)
    {
        super.onPostExecute(result);
        this.callback.onPostExecute(result);
    }

    // Method executed in the MainThread (also called UI Thread)
    // It can be called with publichProgress() method
    @Override
    protected void onProgressUpdate(Integer... values)
    {
        super.onProgressUpdate(values);
        this.callback.onProgressUpdate(values);
    }

    public void executeOnProgressUpdate(Integer... values)
    {
        this.publishProgress(values);
    }
}
