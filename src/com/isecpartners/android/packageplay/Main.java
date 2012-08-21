package com.isecpartners.android.packageplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * An Activity that shows installed packages, allows users to pick what Package
 * to explore and the lets them view those in the ViewPackage Activity. That
 * Activity shows the packages, permissions, activities, services, receivers,
 * providers and Instrumentation on their system. This allows exploration of
 * packages which are installed by third party applications, investigating
 * possible malicious code or malware and see what that or other closed software
 * is up to. Explicit manifest permission declarations (i.e. enforcement by the
 * platform) are shown with: **Permission name. Other permissions may, of
 * course, be enforced by the programs code rather than the runtime system
 * and so do not appear here.
 * 
 * @author Jesse, iSEC Partners, Inc.
 * 
 */
public class Main extends Activity {
	ListView mPkgs = null;
	ArrayList<String> mPackageNames = null;

	/**
	 * Show the list of installed packages, allow users to click what they want
	 * to see. Start the ViewPackage activity for clicked packages.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Map<String, PackageInfo> packages = new HashMap<String, PackageInfo>();
		PackageManager pm = getPackageManager();
		List<PackageInfo> l = pm.getInstalledPackages(0xFFFFFFFF);

		mPkgs = (ListView) findViewById(R.id.package_list);

		for (PackageInfo pi : l)
			packages.put(pi.packageName, pi);

		mPackageNames = new ArrayList<String>(packages.keySet());
		Collections.sort(mPackageNames); // Mr. Palmer prefers order.
		mPackageNames.add(0, "All");
		mPkgs.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mPackageNames));

		mPkgs.setOnItemClickListener(new OnItemClickListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int pos,
					long rowId) {
				Intent i = new Intent();
				ListView lv = (ListView) arg0;
				i.setComponent(new ComponentName(
						"com.isecpartners.android.packageplay",
						"com.isecpartners.android.packageplay.ViewPackage"));
				ArrayList<String> l = new ArrayList<String>();
				String name = (String) lv.getItemAtPosition(pos);
				if (null != name && name.equals("All")) {
					l = (ArrayList<String>) mPackageNames.clone();
					// remove the "All" dummy entry from the list
					l.remove(0);
				} else
					l.add(name);

				i.putExtra("pkgs", l);

				startActivity(i);
			}
		});
	}
}