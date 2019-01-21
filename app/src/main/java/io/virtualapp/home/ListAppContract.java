package io.virtualapp.home;

import java.util.List;

import io.virtualapp.abs.BasePresenter;
import io.virtualapp.abs.BaseView;
import io.virtualapp.home.models.AppInfo;

/**
 * @author Lody
 * @version 1.0
 */
/*package*/ public class ListAppContract {
    public interface ListAppView extends BaseView<ListAppPresenter> {

        void startLoading();

        void loadFinish(List<AppInfo> infoList);

    }

    public interface ListAppPresenter extends BasePresenter {

    }
}
