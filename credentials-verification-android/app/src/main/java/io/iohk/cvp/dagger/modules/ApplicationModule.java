package io.iohk.cvp.dagger.modules;

import androidx.room.Room;

import io.iohk.cvp.core.CvpApplication;
import io.iohk.cvp.data.AppDataManager;
import io.iohk.cvp.data.DataManager;
import io.iohk.cvp.data.local.db.AppDatabase;
import io.iohk.cvp.data.local.db.AppDbHelper;
import io.iohk.cvp.data.local.db.DbHelper;
import io.iohk.cvp.data.local.remote.ApiHelper;
import io.iohk.cvp.data.local.remote.AppApiHelper;
import io.iohk.cvp.utils.Constants;
import io.iohk.cvp.views.Navigator;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class ApplicationModule {

  @Provides
  @Singleton
  public Navigator provideNavigator() {
    return new Navigator();
  }

  @Provides
  @Singleton
  AppDatabase provideAppDatabase(CvpApplication cvpApplication) {
    return Room.databaseBuilder(cvpApplication.getApplicationContext(), AppDatabase.class, Constants.DB_NAME)
            .fallbackToDestructiveMigration()
            .build();
  }

  @Provides
  @Singleton
  DataManager provideDataManager(AppDataManager appDataManager) {
    return appDataManager;
  }

  @Provides
  @Singleton
  DbHelper provideDbHelper(AppDbHelper appDbHelper) {
    return appDbHelper;
  }

  @Provides
  @Singleton
  ApiHelper provideApiHelper(AppApiHelper appApiHelper) {
    return appApiHelper;
  }

}
