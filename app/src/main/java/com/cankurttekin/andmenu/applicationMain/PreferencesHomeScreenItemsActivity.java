package com.cankurttekin.andmenu.applicationMain;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.cankurttekin.andmenu.R;
import com.cankurttekin.andmenu.dataStore.persistent.HomeScreenSetting;
import com.cankurttekin.andmenu.wrapperForAndroid.AppWidgetsHostManager;

import java.util.ArrayList;
import java.util.List;

public class PreferencesHomeScreenItemsActivity extends BaseWindowActivity {
    private AppWidgetsHostManager appWidgetsHostManager = null;
    private MyAdapter myAdapter;

    public PreferencesHomeScreenItemsActivity() {
        super(R.layout.preferences_home_screen_default_items, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.appWidgetsHostManager = new AppWidgetsHostManager(this);

        this.setHeaderFooterTexts(getString(R.string.preferences_title_for_header_home_screen_default_items), getString(R.string.preferences_title_for_footer_home_screen_default_items));
        this.setWindowBoundarySize(ROOT_WINDOW_FULL_WIDTH_IN_MOBILE, 2);

        this.changeBaseWindowElementSizeForAnimation(false);
        this.enableBaseWindowAnimation();

        findViewById(R.id.homeScreenAddQueryButton).setOnClickListener(v ->
                startActivity(new Intent(PreferencesHomeScreenItemsActivity.this, PreferencesHomeScreenEachDefaultItemCommandActivity.class))
        );

        findViewById(R.id.homeScreenAddWidgetButton).setOnClickListener(v ->
                startActivity(new Intent(PreferencesHomeScreenItemsActivity.this, PreferencesHomeScreenAddWidgetActivity.class))
        );

        ListView customListView = findViewById(R.id.homeScreenDefaultItemList);
        this.myAdapter = new MyAdapter(this);
        customListView.setAdapter(myAdapter);
        customListView.setOnItemClickListener((parent, view, position, id) -> {
            ObjectOnList item = myAdapter.getItem(position);
            if (item == null) return;
            if (item.type == TYPE_WIDGET && item.homeScreenWidgetInfo != null) {
                Intent intent = new Intent(PreferencesHomeScreenItemsActivity.this, PreferencesHomeScreenEachWidgetActivity.class);
                intent.putExtra(PreferencesHomeScreenEachWidgetActivity.INTENT_EXTRA_WIDGET_ID, item.homeScreenWidgetInfo.id);
                startActivity(intent);
            } else if (item.type == TYPE_COMMAND && item.homeScreenDefaultItem != null) {
                Intent intent = new Intent(PreferencesHomeScreenItemsActivity.this, PreferencesHomeScreenEachDefaultItemCommandActivity.class);
                intent.putExtra(PreferencesHomeScreenEachDefaultItemCommandActivity.INTENT_EXTRA_ITEM_ID, item.homeScreenDefaultItem.id);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        this.changeBaseWindowElementSizeForAnimation(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.appWidgetsHostManager.garbageCollectForAppWidgetIds();

        this.myAdapter.clear();
        this.myAdapter.addAllWidgets(this.appWidgetsHostManager.fetchHomeScreenAppWidgets(), HomeScreenSetting.getInstance(this).getAllHomeScreenDefaultItems());
    }


    private static final int TYPE_WIDGET = 1;
    private static final int TYPE_COMMAND = 2;
    private static class ObjectOnList {
        public int type = 0;
        public AppWidgetsHostManager.HomeScreenWidgetInfo homeScreenWidgetInfo = null;
        public HomeScreenSetting.HomeScreenDefaultItem homeScreenDefaultItem = null;
    }

    private static class MyAdapter extends ArrayAdapter<ObjectOnList> {
        private final Context context;

        public MyAdapter(Context context) {
            super(context, 0, new ArrayList<>());
            this.context = context;
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            int verticalPadding = (int) (4 * this.context.getResources().getDisplayMetrics().density + 0.5);
            int horizontalPadding = (int) (10 * this.context.getResources().getDisplayMetrics().density + 0.5);
            row.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

            LinearLayout contentLayout = new LinearLayout(context);
            contentLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            contentLayout.setLayoutParams(contentParams);

            TypedValue baseTextColor = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.andmenuBaseTextColor, baseTextColor, true);

            ObjectOnList currentItem = getItem(position);

            TextView titleView = new TextView(this.context);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            int titleViewPadding = (int) (titleView.getTextSize() * 0.3);
            titleView.setPadding(titleViewPadding, titleViewPadding, titleViewPadding, titleViewPadding);
            titleView.setTextColor(baseTextColor.data);
            int type = currentItem != null ? currentItem.type : 0;
            titleView.setText(type == TYPE_WIDGET ? R.string.preferences_home_screen_widget_label : R.string.preferences_home_screen_command_label);
            contentLayout.addView(titleView);

            if (currentItem != null && currentItem.type == TYPE_WIDGET) {
                TextView detailView = new TextView(this.context);
                detailView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                int detailViewPadding = (int) (detailView.getTextSize() * 0.3);
                detailView.setPadding(detailViewPadding, detailViewPadding, detailViewPadding, detailViewPadding);
                detailView.setTextColor(baseTextColor.data);
                if (currentItem.homeScreenWidgetInfo != null && currentItem.homeScreenWidgetInfo.appWidgetProviderInfo != null) {
                    detailView.setText(currentItem.homeScreenWidgetInfo.appWidgetProviderInfo.loadLabel(this.context.getPackageManager()));
                } else {
                    detailView.setText(this.context.getString(R.string.error_failure_could_not_connect_to_the_widget));
                }
                contentLayout.addView(detailView);
            } else if (currentItem != null && currentItem.type == TYPE_COMMAND && currentItem.homeScreenDefaultItem != null) {
                TextView detailView = new TextView(this.context);
                detailView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                int detailViewPadding = (int) (detailView.getTextSize() * 0.3);
                detailView.setPadding(detailViewPadding, detailViewPadding, detailViewPadding, detailViewPadding);
                detailView.setTextColor(baseTextColor.data);
                detailView.setText(currentItem.homeScreenDefaultItem.data);
                contentLayout.addView(detailView);
            }

            row.addView(contentLayout);

            TextView handleView = new TextView(context);
            handleView.setText("≡");
            handleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            handleView.setTextColor(baseTextColor.data);
            handleView.setPadding(24, 12, 24, 12);
            handleView.setGravity(Gravity.CENTER);

            final int currentPos = position;

            handleView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.performClick();
                    ClipData clipData = ClipData.newPlainText("pos", String.valueOf(currentPos));
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(row);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        row.startDragAndDrop(clipData, shadowBuilder, currentPos, 0);
                    } else {
                        row.startDrag(clipData, shadowBuilder, currentPos, 0);
                    }
                    return true;
                }
                return false;
            });

            row.addView(handleView);

            row.setOnDragListener((v, event) -> {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        return true;
                    case DragEvent.ACTION_DRAG_ENTERED:
                        v.setAlpha(0.5f);
                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                        v.setAlpha(1.0f);
                        return true;
                    case DragEvent.ACTION_DROP: {
                        v.setAlpha(1.0f);
                        Object localState = event.getLocalState();
                        if (localState instanceof Integer) {
                            int srcPos = (Integer) localState;
                            if (srcPos != currentPos) {
                                moveItemAndSave(srcPos, currentPos);
                            }
                        }
                        return true;
                    }
                    case DragEvent.ACTION_DRAG_ENDED:
                        v.setAlpha(1.0f);
                        return true;
                }
                return false;
            });

            return row;
        }

        private void moveItemAndSave(int srcPos, int targetPos) {
            if (srcPos < 0 || srcPos >= getCount() || targetPos < 0 || targetPos >= getCount()) {
                return;
            }

            List<ObjectOnList> allItems = new ArrayList<>();
            for (int i = 0; i < getCount(); i++) {
                allItems.add(getItem(i));
            }

            ObjectOnList moved = allItems.remove(srcPos);
            allItems.add(targetPos, moved);

            List<HomeScreenSetting.HomeScreenDefaultItem> dbCommandItems = HomeScreenSetting.getInstance(context).getAllHomeScreenDefaultItems();
            List<ObjectOnList> adapterCommandItems = new ArrayList<>();
            for (ObjectOnList obj : allItems) {
                if (obj.type == TYPE_COMMAND && obj.homeScreenDefaultItem != null) {
                    adapterCommandItems.add(obj);
                }
            }

            for (int i = 0; i < dbCommandItems.size() && i < adapterCommandItems.size(); i++) {
                HomeScreenSetting.HomeScreenDefaultItem dbItem = dbCommandItems.get(i);
                dbItem.data = adapterCommandItems.get(i).homeScreenDefaultItem.data;
                HomeScreenSetting.getInstance(context).updateHomeScreenDefaultItem(dbItem);
                adapterCommandItems.get(i).homeScreenDefaultItem.id = dbItem.id;
            }

            int currentCommandId = -1;
            AppWidgetsHostManager manager = new AppWidgetsHostManager(context);
            for (ObjectOnList obj : allItems) {
                if (obj.type == TYPE_COMMAND && obj.homeScreenDefaultItem != null) {
                    currentCommandId = obj.homeScreenDefaultItem.id;
                } else if (obj.type == TYPE_WIDGET && obj.homeScreenWidgetInfo != null) {
                    obj.homeScreenWidgetInfo.afterDefaultItem = currentCommandId;
                    manager.saveHomeScreenWidgetInfo(obj.homeScreenWidgetInfo);
                }
            }

            clear();
            addAll(allItems);
            notifyDataSetChanged();
        }

        public void addAllWidgets(List<AppWidgetsHostManager.HomeScreenWidgetInfo> allWidgets, List<HomeScreenSetting.HomeScreenDefaultItem> homeScreenDefaultItemList) {
            int widgetId = 0;

            for (HomeScreenSetting.HomeScreenDefaultItem item : homeScreenDefaultItemList) {
                while (widgetId < allWidgets.size() && allWidgets.get(widgetId).afterDefaultItem < item.id) {
                    ObjectOnList objectOnList = new ObjectOnList();
                    objectOnList.type = TYPE_WIDGET;
                    objectOnList.homeScreenWidgetInfo = allWidgets.get(widgetId);

                    this.add(objectOnList);

                    ++widgetId;
                }
                ObjectOnList objectOnList = new ObjectOnList();
                objectOnList.type = TYPE_COMMAND;
                objectOnList.homeScreenDefaultItem = item;

                this.add(objectOnList);
            }

            while (widgetId < allWidgets.size()) {
                ObjectOnList objectOnList = new ObjectOnList();
                objectOnList.type = TYPE_WIDGET;
                objectOnList.homeScreenWidgetInfo = allWidgets.get(widgetId);

                this.add(objectOnList);

                ++widgetId;
            }
        }
    }
}
