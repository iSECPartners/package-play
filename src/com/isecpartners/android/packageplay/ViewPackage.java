package com.isecpartners.android.packageplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * An Activity that shows the details of a selected package or packages. takes a
 * list of package names in the extra "pkgs", if only one is provided, the
 * resulting UI allows starting its actions.
 * 
 * @author Jesse
 * 
 */
public class ViewPackage extends Activity {
	Spinner mActivities = null;
	Button mStartButton = null;
	Button mManifestButton = null;
	Button mSystemViewButton = null;
	TextView mLabel = null;
	List<String> mActivitiesList = new ArrayList<String>();
	TextView mOut = null;
	String mPkgName = null;
	Map<String, PackageInfo> packages = new HashMap<String, PackageInfo>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.textdesc);
		initControls();

		PackageManager pm = getPackageManager();
		List<PackageInfo> l = pm.getInstalledPackages(0xFFFFFFFF);
		for (PackageInfo pi : l)
			packages.put(pi.packageName, pi);

		Intent startingIntent = this.getIntent();

		try {
			ArrayList<String> toShow = startingIntent.getExtras()
					.getStringArrayList("pkgs");
			/*
			 * Allow starting of activities when we are in single package view
			 * mode, and give package count when not exactly one.
			 */
			if (toShow.size() != 1)
				mOut.append("Received " + toShow.size() + " package names\n");
			else
				setupActionStuff(pm.getPackageInfo(toShow.get(0), 0xFFFFFFFF));

			for (String cur : toShow) {
				mOut.append(describePackage(packages.get(cur)));
			}

		} catch (Exception e) {
			mOut.append("Error handling pkgs list: " + e.getMessage());
		}
	}

	void initControls() {
		mActivities = (Spinner) findViewById(R.id.activities);
		mStartButton = (Button) findViewById(R.id.startActivity);
		mManifestButton = (Button) findViewById(R.id.viewManifest);
		mSystemViewButton = (Button) findViewById(R.id.viewSystem);
		mOut = (TextView) findViewById(R.id.output);
		mLabel = (TextView) findViewById(R.id.activityLabel);

		showActivityUI(false);

		mStartButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startCurrentlySelected();
			}
		});

		mManifestButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i
						.setClassName(
								"com.isecpartners.android.manifestexplorer",
								"com.isecpartners.android.manifestexplorer.ManifestExplorer");
				i.putExtra("PackageToView", mPkgName);
				startActivity(i);
			}
		});
		mSystemViewButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
                /*
                 * Note: used to be "com.android.settings.InstalledAppDetails".
                 * The new way is to pass in a package URL.  I'm keeping the old
                 * putExtra() for now in case it helps backwards compat. 
                 */
                try {
				    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("package:" + mPkgName));
				    i.setClassName("com.android.settings",
				    		"com.android.settings.applications.InstalledAppDetails");
				    i.putExtra("com.android.settings.ApplicationPkgName", mPkgName);
				    startActivity(i);
                } catch(ActivityNotFoundException e) {
                    /* for very old android, try the old way... */
				    Intent i = new Intent(Intent.ACTION_VIEW);
				    i.setClassName("com.android.settings",
				    		"com.android.settings.InstalledAppDetails");
				    i.putExtra("com.android.settings.ApplicationPkgName", mPkgName);
				    startActivity(i);
                }
			}
		});

	}

	public void showActivityUI(boolean shouldShow) {
		mStartButton.setEnabled(shouldShow);
		mActivities.setEnabled(shouldShow);
		if (shouldShow) {
			mStartButton.setVisibility(View.VISIBLE);
			mActivities.setVisibility(View.VISIBLE);
			mLabel.setVisibility(View.VISIBLE);
		} else {
			mStartButton.setVisibility(View.GONE);
			mActivities.setVisibility(View.GONE);
			mLabel.setVisibility(View.GONE);
		}
	}

	/**
	 * Populates the activity selection spinner and activates start button if
	 * needed.
	 */
	public void setupActionStuff(PackageInfo p) {
		showActivityUI(false);
		mActivitiesList.clear();
		mPkgName = null;

		// enable system and manifest views as this is an individual package
		mSystemViewButton.setVisibility(View.VISIBLE);
		try {
			getPackageManager().getPackageGids(
					"com.isecpartners.android.manifestexplorer");
			mManifestButton.setVisibility(View.VISIBLE);
		} catch (NameNotFoundException e) {
			mManifestButton.setVisibility(View.GONE);
		}

		if (null == p || null == p.activities)
			return;

		mPkgName = p.packageName;

		for (ActivityInfo ai : p.activities)
			if (ai.exported)
				mActivitiesList.add(ai.name);

		ArrayAdapter<String> AA = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_dropdown_item, mActivitiesList);
		mActivities.setAdapter(AA);

		if (mActivities.getCount() > 0)
			showActivityUI(true);
	}

	/**
	 * Looks at the spinner for the currently selected Activity, then starts it.
	 */
	public void startCurrentlySelected() {
		Intent i = new Intent();
		i.setComponent(new ComponentName(mPkgName, mActivities
				.getSelectedItem().toString()));
		startActivity(i);
	}

	/**
	 * Provide a detailed Textual description of a package in English.
	 * 
	 * ToDo: Break this code up so it is shorter and uses reuses a method or
	 * two. Eliminate bad trailing commas which happen when the last object
	 * isn't exported, or for instrumentations.
	 * 
	 * @param pi
	 *            Package information for the package to describe.
	 * @return Text describing the package or the error encountered when
	 *         generating it.
	 */
	public CharSequence describePackage(PackageInfo pi) {
		String perm = null;
		StringBuffer sb = new StringBuffer();
		try {
			sb.append("\nPackage Name: " + pi.packageName);

			if (pi.requestedPermissions != null) {
				sb.append("\n\nUses Permissions: ");
				for (int i = 0; i < pi.requestedPermissions.length; i++) {
					sb.append(pi.requestedPermissions[i]
							+ ((i < pi.requestedPermissions.length - 1) ? ", "
									: ""));
				}
			} else {
				sb.append("\n\nPackage uses no permssions.");
			}
			if (pi.applicationInfo != null && !pi.applicationInfo.enabled)
				sb.append("\n\nPackage currently DISABLED");

			if (pi.permissions != null) {
				sb.append("\n\nDefines Permissions: ");
				for (int i = 0; i < pi.permissions.length; i++) {
					sb.append(pi.permissions[i].name
							+ ((i < pi.permissions.length - 1) ? ", " : ""));
				}
			} else {
				sb.append("\n\nPackage defines no new permssions.");
			}

			if (pi.activities != null) {
				sb.append("\n\nExported Activities: ");
				for (int i = 0; i < pi.activities.length; i++) {
					if (pi.activities[i].exported) {
						if (pi.activities[i].name == null
								|| pi.activities[i].name.length() == 0)
							sb.append("NO_NAME_FOR_ACTIVITY");
						else
							sb.append(pi.activities[i].name);
						perm = pi.activities[i].permission;
						if (null != perm)
							sb.append(" **" + perm);
						if (i < pi.activities.length - 1)
							sb.append(", ");
					}
				}

				sb.append("\n\nNon-Exported Activities: ");
				for (int i = 0; i < pi.activities.length; i++) {
					if (!pi.activities[i].exported) {
						if (pi.activities[i].name == null
								|| pi.activities[i].name.length() == 0)
							sb.append("NO_NAME_FOR_ACTIVITY");
						else
							sb.append(pi.activities[i].name);
						if (i < pi.activities.length - 1)
							sb.append(", ");
					}
				}
			}

			if (pi.services != null) {
				sb.append("\n\nExported Services: ");
				for (int i = 0; i < pi.services.length; i++) {
					if (pi.services[i].exported) {
						sb.append(pi.services[i].name);
						perm = pi.services[i].permission;
						if (null != perm)
							sb.append(" **" + perm);
						if (i < pi.services.length - 1)
							sb.append(", ");
					}
				}
			}

			if (pi.receivers != null) {
				sb.append("\n\nExported Broadcast Receivers: ");
				for (int i = 0; i < pi.receivers.length; i++) {
					ActivityInfo ai = pi.receivers[i];
					if (ai.exported) {
						sb.append(ai.name);
						perm = ai.permission;
						if (null != perm)
							sb.append(" **" + perm);
						if (i < pi.receivers.length - 1)
							sb.append(", ");
					}
				}
			}

			if (pi.providers != null) {
				sb.append("\n\nExported Content Providers: ");
				for (int i = 0; i < pi.providers.length; i++) {
					if (pi.providers[i].exported) {
						sb.append(pi.providers[i].name);
						perm = pi.providers[i].readPermission;
						String wPerm = pi.providers[i].writePermission;
						if (null != perm)
							sb.append(" *Read*" + perm);
						if (null != wPerm)
							sb.append(" *Write*" + wPerm);
						if (i < pi.providers.length - 1)
							sb.append(", ");
					}
				}
			}

			if (pi.instrumentation != null) {
				sb.append("\n\nInstrumentations: ");

				for (int i = 0; i < pi.instrumentation.length; i++) {
					sb.append(pi.instrumentation[i].name + ", ");
				}
			}

			if (pi.gids != null && pi.gids.length > 0) {
				sb.append("\n\nGids: ");
				for (int gid : pi.gids)
					sb.append(gid + " ");
			}

			if (pi.applicationInfo != null) {
				sb.append("\n\nApplication Infomation:");
				if (pi.applicationInfo.permission != null)
					sb.append("\nApplication permission: "
							+ pi.applicationInfo.permission);

				if (pi.applicationInfo.publicSourceDir != null)
					sb.append("\nPublic Source Dir: "
							+ pi.applicationInfo.publicSourceDir);

				if (pi.applicationInfo.sharedLibraryFiles != null
						&& pi.applicationInfo.sharedLibraryFiles.length > 0) {
					sb.append("\nShared library files: ");
					for (String lib : pi.applicationInfo.sharedLibraryFiles)
						sb.append("\n\t" + lib);
				}
			}
			sb.append("\n");
		} catch (Exception e) {
			sb.append("error: " + e.getMessage() + " on "
					+ (null == pi.packageName ? "package name" : "activities"));
		}
		return sb;
	} // describePackage
}
