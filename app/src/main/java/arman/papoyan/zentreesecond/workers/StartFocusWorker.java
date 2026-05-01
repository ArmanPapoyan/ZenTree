package arman.papoyan.zentreesecond.workers;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import arman.papoyan.zentreesecond.services.FocusForegroundService;

public class StartFocusWorker extends Worker {

    public StartFocusWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Intent intent = new Intent(getApplicationContext(), FocusForegroundService.class);
        getApplicationContext().startService(intent);
        return Result.success();
    }
}