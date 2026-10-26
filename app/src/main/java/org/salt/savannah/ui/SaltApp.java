package org.salt.savannah.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.salt.savannah.R;
import org.salt.savannah.security.FaceBlur;
import org.salt.savannah.security.KeyAttemptPolicy;
import org.salt.savannah.security.LoginGuard;
import org.salt.savannah.security.PostLimiter;
import org.salt.savannah.security.SaltCrypto;

/** Android screens measured to the iOS SALT layout: login, tabs, feed, and the security controls. */
public final class SaltApp {
    public static final int PHOTO_REQUEST = 7;
    private static final String TAGLINE = "Strengthening Savannah's Local Tourism.\nCommitted to Its Future.\nInvested in Savannah.";
    private static final String[] TAB_TITLES = {"Home", "Directory", "Events", "Jobs", "More"};
    private static final String[] TAB_ICONS = {"ic_house", "ic_people", "ic_calendar", "ic_briefcase", "ic_more"};

    private final Activity activity;
    private final SaltLook look;
    private final LoginGuard loginGuard = new LoginGuard();
    private final PostLimiter postLimiter = new PostLimiter();
    private final List<View> pages = new ArrayList<>();
    private final List<GroupChat> groups = new ArrayList<>();
    private final FrameLayout content;
    private final LinearLayout tabBar;
    public final FrameLayout root;
    private boolean signedIn;
    private int tab;
    private String directoryFilter = "All";
    private String eventFilter = "All";
    private String resourceFilter = "All";
    private String directoryQuery = "";
    private String jobQuery = "";
    private byte[] pendingOriginal;
    private byte[] publicPhoto;
    private boolean facesFound;
    private TextView composerStatus;
    private ImageView composerPreview;

    public SaltApp(Activity activity) {
        this.activity = activity;
        this.look = new SaltLook(activity);
        this.content = new FrameLayout(activity);
        this.tabBar = buildTabBar();
        LinearLayout column = look.column();
        column.setFitsSystemWindows(true);
        content.setBackgroundColor(SaltLook.BACKGROUND);
        column.addView(content, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        column.addView(tabBar);
        root = new FrameLayout(activity);
        root.setBackgroundColor(SaltLook.BACKGROUND);
        root.addView(column);
        showLogin();
    }

    public boolean onBack() {
        if (pages.size() <= 1) return false;
        pages.remove(pages.size() - 1);
        discardPhoto();
        if (pages.size() == 1 && signedIn) {
            showTab(tab);
        } else {
            mount(pages.get(pages.size() - 1));
        }
        return true;
    }

    public void onPhoto(Intent data) {
        if (data == null || data.getData() == null || composerStatus == null || !composerStatus.isAttachedToWindow()) return;
        Uri uri = data.getData();
        try (InputStream input = activity.getContentResolver().openInputStream(uri)) {
            if (input == null) throw new SecurityException("image rejected");
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = input.read(chunk)) >= 0) raw.write(chunk, 0, read);
            byte[] original = raw.toByteArray();
            if (BitmapFactory.decodeByteArray(original, 0, original.length) == null) {
                throw new SecurityException("image rejected");
            }
            FaceBlur.Prepared prepared = FaceBlur.prepare(activity, original, false);
            discardPhoto();
            pendingOriginal = original;
            publicPhoto = prepared.publicJpeg;
            facesFound = prepared.facesFound;
            composerPreview.setImageBitmap(BitmapFactory.decodeByteArray(publicPhoto, 0, publicPhoto.length));
            composerPreview.setVisibility(View.VISIBLE);
            composerStatus.setText(facesFound
                ? "Faces are blurred on the public photo. The original is only held until you send it for consent review or leave this screen."
                : "No face found. Only the scanned image is kept.");
        } catch (Exception ex) {
            discardPhoto();
            composerStatus.setText("The photo was not uploaded because faces could not be scanned.");
        }
    }

    private void showLogin() {
        signedIn = false;
        tabBar.setVisibility(View.GONE);
        pages.clear();
        push(loginPage(), false);
    }

    private void enterApp() {
        signedIn = true;
        tabBar.setVisibility(View.VISIBLE);
        tab = 0;
        paintTabs();
        showTab(0);
    }

    private void showTab(int index) {
        tab = index;
        paintTabs();
        pages.clear();
        View page;
        if (index == 0) page = homePage();
        else if (index == 1) page = directoryPage();
        else if (index == 2) page = eventsPage();
        else if (index == 3) page = jobsPage();
        else page = morePage();
        push(page, false);
    }

    private void push(View page, boolean rememberPhoto) {
        if (!rememberPhoto) discardPhoto();
        pages.add(page);
        mount(page);
    }

    private void open(View page) {
        push(page, false);
    }

    private void mount(View page) {
        content.removeAllViews();
        content.addView(page, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private View loginPage() {
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.setPadding(look.dp(24), 0, look.dp(24), look.dp(40));
        ImageView logo = new ImageView(activity);
        logo.setImageResource(R.drawable.salt_logo);
        logo.setAdjustViewBounds(true);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(look.dp(96), look.dp(96));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        logoParams.topMargin = look.dp(48);
        body.addView(logo, logoParams);
        TextView title = look.text("SALT", 34, 700, true, SaltLook.PURPLE);
        title.setGravity(Gravity.CENTER);
        look.add(body, title, 12);
        TextView subtitle = look.text("Savannah Area Local Tours", 15, 400, false, SaltLook.SECONDARY_LABEL);
        subtitle.setGravity(Gravity.CENTER);
        look.add(body, subtitle, 12);
        TextView tagline = look.text(TAGLINE, 17, 600, true, SaltLook.GOLD);
        tagline.setGravity(Gravity.CENTER);
        tagline.setLineSpacing(look.dp(3), 1f);
        look.add(body, tagline, 16);
        EditText email = look.input("you@business.com", false);
        EditText password = look.input("Password", true);
        TextView error = look.text("", 13, 400, false, SaltLook.RED);
        error.setVisibility(View.GONE);
        look.add(body, field("Email", email), 40);
        look.add(body, passwordRow(password), 16);
        look.add(body, error, 8);
        TextView signIn = look.button("Sign In", SaltLook.PURPLE, android.graphics.Color.WHITE);
        look.add(body, signIn, 8);
        TextView forgot = look.text("Forgot Password?", 15, 400, false, SaltLook.PURPLE);
        forgot.setGravity(Gravity.CENTER);
        look.press(forgot, () -> alert("Reset Password", "Password reset link will be sent to your email (demo mode)."));
        look.add(body, forgot, 16);
        look.add(body, orDivider(), 16);
        TextView apply = look.button("Apply for Membership", SaltLook.alpha(SaltLook.PURPLE, 0.12f), SaltLook.PURPLE);
        look.press(apply, () -> open(formPage("Apply for Membership", membershipForm())));
        look.add(body, apply, 16);
        TextView create = look.text("Create an Account", 15, 400, false, SaltLook.PURPLE);
        create.setGravity(Gravity.CENTER);
        look.press(create, () -> open(formPage("Create Account", createAccountForm())));
        look.add(body, create, 16);
        TextView demo = look.text("Demo: demo@salt.savannah / SavannahTour1", 12, 400, false, SaltLook.SECONDARY_LABEL);
        demo.setGravity(Gravity.CENTER);
        look.add(body, demo, 32);
        look.press(signIn, () -> attemptLogin(email.getText().toString(), password.getText().toString(), error));
        scroll.addView(body);
        return scroll;
    }

    private void attemptLogin(String email, String password, TextView error) {
        long now = System.currentTimeMillis();
        try {
            loginGuard.begin(email, now);
        } catch (SecurityException ex) {
            error.setText("Too many sign-in attempts. Try again after 15 minutes.");
            error.setVisibility(View.VISIBLE);
            return;
        }
        boolean ok = email.contains("@") && password.length() >= 12;
        loginGuard.record(email, ok, now);
        if (!ok) {
            error.setText("Invalid email or password.");
            error.setVisibility(View.VISIBLE);
            return;
        }
        SaltData.current = SaltData.members.get(0);
        enterApp();
    }

    private View homePage() {
        LinearLayout page = look.column();
        page.addView(inlineBar());
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.setPadding(0, look.dp(16), 0, look.dp(24));
        LinearLayout banner = look.column();
        banner.setGravity(Gravity.CENTER_HORIZONTAL);
        banner.setPadding(look.dp(16), look.dp(16), look.dp(16), look.dp(16));
        GradientDrawable wash = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {
            SaltLook.alpha(SaltLook.PURPLE, 0.15f), SaltLook.alpha(SaltLook.PURPLE, 0.05f)
        });
        wash.setCornerRadius(look.dp(16));
        banner.setBackground(wash);
        TextView welcome = look.text("Welcome back", 15, 400, false, SaltLook.SECONDARY_LABEL);
        welcome.setGravity(Gravity.CENTER);
        banner.addView(welcome);
        TextView name = look.text(SaltData.current.displayName(), 22, 600, true, SaltLook.LABEL);
        name.setGravity(Gravity.CENTER);
        look.add(banner, name, 8);
        TextView tagline = look.text(TAGLINE, 17, 600, true, SaltLook.GOLD);
        tagline.setGravity(Gravity.CENTER);
        tagline.setLineSpacing(look.dp(3), 1f);
        look.add(banner, tagline, 8);
        LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bannerParams.leftMargin = look.dp(16);
        bannerParams.rightMargin = look.dp(16);
        body.addView(banner, bannerParams);
        body.addView(quickActions());
        LinearLayout header = look.row(Gravity.CENTER_VERTICAL);
        header.setPadding(look.dp(16), look.dp(8), look.dp(16), 0);
        TextView feed = look.text("Community Feed", 17, 600, true, SaltLook.LABEL);
        header.addView(feed, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if ("active".equals(SaltData.current.status)) {
            ImageView plus = look.icon("ic_plus_circle", SaltLook.PURPLE, 28);
            look.press(plus, () -> open(composerPage()));
            header.addView(plus);
        }
        body.addView(header);
        if (!"active".equals(SaltData.current.status)) {
            TextView locked = look.text("Only active SALT members can post.", 12, 400, false, SaltLook.SECONDARY_LABEL);
            locked.setPadding(look.dp(16), look.dp(8), look.dp(16), 0);
            body.addView(locked);
        }
        for (SaltData.Post post : SaltData.posts) body.addView(postCard(post));
        scroll.addView(body);
        page.addView(scroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        return page;
    }

    private View quickActions() {
        HorizontalScrollView scroller = new HorizontalScrollView(activity);
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout row = look.row(Gravity.TOP);
        row.setClipChildren(false);
        row.setPadding(look.dp(16), look.dp(16), look.dp(16), 0);
        row.addView(action("Events", "ic_calendar", SaltLook.PURPLE, false, () -> showTab(2)));
        row.addView(action("Directory", "ic_people", SaltLook.RED, false, () -> showTab(1)));
        row.addView(action("Jobs", "ic_briefcase", SaltLook.GOLD, false, () -> showTab(3)));
        row.addView(action("Resources", "ic_book", SaltLook.PURPLE, true, this::openResources));
        scroller.addView(row);
        return scroller;
    }

    private View action(String title, String icon, int color, boolean united, Runnable tap) {
        LinearLayout box = look.column();
        box.setPadding(0, 0, look.dp(12), 0);
        LinearLayout icons = look.row(Gravity.CENTER_VERTICAL);
        FrameLayout tile = iconTile(icon, color, 52, 14);
        icons.addView(tile);
        if (united) {
            ImageView home = new ImageView(activity);
            home.setImageResource(R.drawable.united_home);
            home.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(look.dp(52), look.dp(52));
            params.leftMargin = look.dp(56);
            home.setLayoutParams(params);
            clip(home, 14);
            icons.addView(home);
        }
        box.addView(icons);
        box.setClipChildren(false);
        TextView label = look.text(title, 12, 400, false, SaltLook.LABEL);
        label.setSingleLine(true);
        label.setGravity(Gravity.CENTER);
        label.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = look.dp(8);
        labelParams.leftMargin = Math.min(0, (look.dp(52) - label.getMeasuredWidth()) / 2);
        box.addView(label, labelParams);
        look.press(box, tap);
        return box;
    }

    private View postCard(SaltData.Post post) {
        LinearLayout card = look.column();
        card.setPadding(look.dp(16), look.dp(16), look.dp(16), look.dp(16));
        card.setBackground(look.rounded(SaltLook.SECONDARY, 16));
        LinearLayout author = look.row(Gravity.CENTER_VERTICAL);
        author.addView(avatar(post.author, 44, 17));
        LinearLayout names = look.column();
        LinearLayout titleRow = look.row(Gravity.CENTER_VERTICAL);
        titleRow.addView(look.text(post.author, 17, 600, true, SaltLook.LABEL));
        if (post.official) titleRow.addView(capsule("OFFICIAL", 10, android.graphics.Color.WHITE, SaltLook.RED, 6, 2));
        if (post.pinned) {
            ImageView pin = look.icon("ic_pin", SaltLook.GOLD, 12);
            LinearLayout.LayoutParams pinParams = (LinearLayout.LayoutParams) pin.getLayoutParams();
            pinParams.leftMargin = look.dp(6);
            titleRow.addView(pin);
        }
        names.addView(titleRow);
        if (post.business != null) {
            TextView business = look.text(post.business, 12, 400, false, SaltLook.SECONDARY_LABEL);
            look.add(names, business, 2);
        }
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameParams.leftMargin = look.dp(12);
        author.addView(names, nameParams);
        author.addView(look.text(SaltData.ago(post.created), 12, 400, false, SaltLook.SECONDARY_LABEL));
        card.addView(author);
        TextView content = look.text(post.content, 17, 400, false, SaltLook.LABEL);
        look.add(card, content, 12);
        if (post.image != null) {
            ImageView photo = new ImageView(activity);
            photo.setImageBitmap(BitmapFactory.decodeByteArray(post.image, 0, post.image.length));
            photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, look.dp(140));
            photoParams.topMargin = look.dp(12);
            card.addView(photo, photoParams);
            clip(photo, 12);
        }
        LinearLayout actions = look.row(Gravity.CENTER_VERTICAL);
        boolean liked = post.liked;
        ImageView heart = look.icon(liked ? "ic_heart" : "ic_heart_outline", liked ? SaltLook.RED : SaltLook.SECONDARY_LABEL, 18);
        TextView likes = look.text(String.valueOf(post.likes), 12, 400, false, SaltLook.SECONDARY_LABEL);
        LinearLayout like = look.row(Gravity.CENTER_VERTICAL);
        like.addView(heart);
        LinearLayout.LayoutParams likeCount = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        likeCount.leftMargin = look.dp(4);
        like.addView(likes, likeCount);
        look.press(like, () -> {
            post.liked = !post.liked;
            post.likes += post.liked ? 1 : -1;
            heart.setImageResource(post.liked ? R.drawable.ic_heart : R.drawable.ic_heart_outline);
            heart.setColorFilter(post.liked ? SaltLook.RED : SaltLook.SECONDARY_LABEL);
            likes.setText(String.valueOf(post.likes));
        });
        actions.addView(like);
        ImageView bubble = look.icon("ic_bubble", SaltLook.SECONDARY_LABEL, 18);
        TextView comments = look.text(String.valueOf(post.comments), 12, 400, false, SaltLook.SECONDARY_LABEL);
        LinearLayout comment = look.row(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams commentParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        commentParams.leftMargin = look.dp(24);
        comment.addView(bubble);
        LinearLayout.LayoutParams commentCount = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        commentCount.leftMargin = look.dp(4);
        comment.addView(comments, commentCount);
        look.press(comment, () -> open(commentsPage(post)));
        actions.addView(comment, commentParams);
        look.add(card, actions, 12);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.leftMargin = look.dp(16);
        cardParams.rightMargin = look.dp(16);
        cardParams.topMargin = look.dp(16);
        card.setLayoutParams(cardParams);
        return card;
    }

    private View directoryPage() {
        LinearLayout page = look.column();
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.addView(largeTitle("Member Directory"));
        EditText search = searchField("Search members or businesses");
        search.setText(directoryQuery);
        body.addView(searchWrap(search));
        HorizontalScrollView chips = chips(SaltData.DIRECTORY, directoryFilter, value -> {
            directoryFilter = value;
            showTab(1);
        });
        body.addView(chips);
        LinearLayout results = look.column();
        body.addView(results);
        refreshDirectory(results);
        search.addTextChangedListener(watcher(value -> {
            directoryQuery = value;
            refreshDirectory(results);
        }));
        scroll.addView(body);
        page.addView(scroll);
        return page;
    }

    private void refreshDirectory(LinearLayout results) {
        results.removeAllViews();
        List<SaltData.Member> shown = new ArrayList<>();
        for (SaltData.Member member : SaltData.members) {
            boolean category = "All".equals(directoryFilter) || member.categories.equals(directoryFilter);
            String haystack = (member.displayName() + " " + member.fullName() + " " + member.bio).toLowerCase(Locale.US);
            boolean query = directoryQuery.isEmpty() || haystack.contains(directoryQuery.toLowerCase(Locale.US));
            if (category && query) shown.add(member);
        }
        if (shown.isEmpty()) results.addView(empty("No Members Found", "Try adjusting your search or filters."));
        for (SaltData.Member member : shown) results.addView(memberRow(member));
    }

    private View memberRow(SaltData.Member member) {
        LinearLayout row = look.row(Gravity.CENTER_VERTICAL);
        row.setPadding(look.dp(16), look.dp(12), look.dp(16), look.dp(12));
        row.addView(avatar(member.displayName(), 52, 20));
        LinearLayout text = look.column();
        LinearLayout title = look.row(Gravity.CENTER_VERTICAL);
        TextView memberName = look.text(member.displayName(), 17, 600, true, SaltLook.LABEL);
        memberName.setSingleLine(true);
        memberName.setEllipsize(TextUtils.TruncateAt.END);
        title.addView(memberName, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (member.board) title.addView(capsule("BOARD", 9, android.graphics.Color.WHITE, SaltLook.GOLD, 5, 2));
        text.addView(title);
        TextView category = look.text(member.categories, 12, 400, false, SaltLook.SECONDARY_LABEL);
        category.setSingleLine(true);
        look.add(text, category, 4);
        TextView bio = look.text(member.bio, 13, 400, false, SaltLook.SECONDARY_LABEL);
        bio.setSingleLine(true);
        look.add(text, bio, 4);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.leftMargin = look.dp(14);
        row.addView(text, params);
        row.addView(look.icon("ic_chevron", SaltLook.SECONDARY_LABEL, 18));
        look.press(row, () -> open(memberPage(member)));
        LinearLayout wrap = look.column();
        wrap.addView(row);
        wrap.addView(insetLine());
        return wrap;
    }

    private View memberPage(SaltData.Member member) {
        LinearLayout page = navPage(member.displayName());
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.setPadding(look.dp(16), look.dp(16), look.dp(16), look.dp(32));
        LinearLayout header = look.column();
        header.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout face = avatar(member.displayName(), 96, 36);
        LinearLayout.LayoutParams faceParams = new LinearLayout.LayoutParams(look.dp(96), look.dp(96));
        faceParams.gravity = Gravity.CENTER_HORIZONTAL;
        header.addView(face, faceParams);
        TextView display = look.text(member.displayName(), 22, 600, true, SaltLook.LABEL);
        display.setGravity(Gravity.CENTER);
        look.add(header, display, 12);
        TextView full = look.text(member.fullName(), 15, 400, false, SaltLook.SECONDARY_LABEL);
        full.setGravity(Gravity.CENTER);
        look.add(header, full, 12);
        if (member.board) {
            TextView board = look.text("Board of Directors", 12, 400, false, SaltLook.GOLD);
            board.setGravity(Gravity.CENTER);
            look.add(header, board, 12);
        }
        TextView tier = capsule(member.tier.equals("For-Profit Business") ? member.tier : member.tier, 12, SaltLook.PURPLE, SaltLook.alpha(SaltLook.PURPLE, 0.12f), 12, 6);
        tier.setGravity(Gravity.CENTER);
        look.add(header, tier, 12);
        body.addView(header);
        body.addView(cardBlock("About", look.text(member.bio, 17, 400, false, SaltLook.SECONDARY_LABEL)));
        LinearLayout contact = look.column();
        if (member.phone != null) contact.addView(infoRow("ic_phone", member.phone, SaltLook.LABEL, null));
        if (member.website != null) {
            String site = member.website.replace("https://", "");
            look.add(contact, infoRow("ic_globe", site, SaltLook.PURPLE, () -> browse(member.website)), 12);
        }
        look.add(contact, infoRow("ic_mail", member.email, SaltLook.LABEL, null), member.phone == null && member.website == null ? 0 : 12);
        body.addView(cardBlock("Contact", contact));
        TextView message = look.button("Message Member", SaltLook.PURPLE, android.graphics.Color.WHITE);
        look.press(message, () -> open(chatPage(member)));
        look.add(body, message, 24);
        scroll.addView(body);
        page.addView(scroll, match());
        return page;
    }

    private View eventsPage() {
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.addView(largeTitle("Events"));
        body.addView(chips(SaltData.EVENT_FILTERS, eventFilter, value -> {
            eventFilter = value;
            showTab(2);
        }));
        boolean any = false;
        for (SaltData.Event event : SaltData.events) {
            if (!"All".equals(eventFilter) && !event.category.equals(eventFilter)) continue;
            any = true;
            body.addView(eventRow(event));
        }
        if (!any) body.addView(empty("No Upcoming Events", ""));
        scroll.addView(body);
        return scroll;
    }

    private View eventRow(SaltData.Event event) {
        LinearLayout row = look.row(Gravity.TOP);
        row.setPadding(look.dp(16), look.dp(14), look.dp(16), look.dp(14));
        LinearLayout badge = look.column();
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(look.rounded(SaltLook.alpha(SaltLook.PURPLE, 0.10f), 10));
        badge.setPadding(0, look.dp(8), 0, look.dp(8));
        TextView month = look.text(SaltData.month(event.start), 12, 400, false, SaltLook.PURPLE);
        month.setGravity(Gravity.CENTER);
        TextView day = look.text(SaltData.day(event.start), 22, 600, true, SaltLook.PURPLE);
        day.setGravity(Gravity.CENTER);
        badge.addView(month);
        badge.addView(day);
        row.addView(badge, new LinearLayout.LayoutParams(look.dp(52), LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout text = look.column();
        LinearLayout title = look.row(Gravity.CENTER_VERTICAL);
        TextView name = look.text(event.title, 17, 600, true, SaltLook.LABEL);
        name.setMaxLines(2);
        title.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (event.signature) title.addView(look.icon("ic_star", SaltLook.GOLD, 12));
        text.addView(title);
        look.add(text, infoRow("ic_place", event.location, SaltLook.SECONDARY_LABEL, null), 6);
        LinearLayout meta = look.row(Gravity.CENTER_VERTICAL);
        meta.addView(look.text(SaltData.mediumTime(event.start), 12, 400, false, SaltLook.SECONDARY_LABEL),
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (event.rsvped) meta.addView(capsule("RSVP'd", 12, SaltLook.PURPLE, SaltLook.alpha(SaltLook.PURPLE, 0.12f), 8, 3));
        else if (event.memberPrice != null) {
            String price = event.memberPrice == 0 ? "Free for Members" : "$" + event.memberPrice + " members";
            meta.addView(look.text(price, 12, 400, false, SaltLook.GOLD));
        }
        look.add(text, meta, 6);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = look.dp(14);
        row.addView(text, textParams);
        look.press(row, () -> open(eventPage(event)));
        LinearLayout wrap = look.column();
        wrap.addView(row);
        wrap.addView(insetLine());
        return wrap;
    }

    private View eventPage(SaltData.Event event) {
        LinearLayout page = navPage(event.title);
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.setPadding(look.dp(16), look.dp(16), look.dp(16), look.dp(32));
        if (event.signature) body.addView(look.text("Signature Event", 12, 400, false, SaltLook.GOLD));
        TextView title = look.text(event.title, 28, 700, true, SaltLook.LABEL);
        look.add(body, title, 8);
        look.add(body, infoRow("ic_calendar", SaltData.mediumTime(event.start), SaltLook.SECONDARY_LABEL, null), 8);
        look.add(body, infoRow("ic_place", event.location, SaltLook.SECONDARY_LABEL, null), 8);
        TextView description = look.text(event.description, 17, 400, false, SaltLook.LABEL);
        look.add(body, description, 20);
        LinearLayout pricing = look.column();
        pricing.addView(look.text("Pricing", 17, 600, true, SaltLook.LABEL));
        LinearLayout prices = look.row(Gravity.CENTER_VERTICAL);
        prices.addView(priceBlock("Members", money(event.memberPrice), SaltLook.PURPLE),
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        prices.addView(priceBlock("Non-Members", money(event.guestPrice), SaltLook.SECONDARY_LABEL));
        look.add(pricing, prices, 8);
        body.addView(cardWrap(pricing));
        if (event.max != null) {
            TextView count = look.text(event.attending + " / " + event.max + " attending", 15, 400, false, SaltLook.LABEL);
            look.add(body, count, 16);
        }
        boolean full = event.max != null && event.attending >= event.max && !event.rsvped;
        TextView rsvp = look.button(event.rsvped ? "Cancel RSVP" : (full ? "Event Full" : "RSVP Now"),
            event.rsvped ? SaltLook.alpha(SaltLook.PURPLE, 0.12f) : SaltLook.PURPLE,
            event.rsvped ? SaltLook.PURPLE : android.graphics.Color.WHITE);
        look.press(rsvp, () -> {
            if (full) return;
            event.rsvped = !event.rsvped;
            event.attending += event.rsvped ? 1 : -1;
            open(eventPage(event));
            pages.remove(pages.size() - 2);
        });
        look.add(body, rsvp, 16);
        scroll.addView(body);
        page.addView(scroll, match());
        return page;
    }

    private View jobsPage() {
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        LinearLayout titleRow = look.row(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(look.dp(16), look.dp(8), look.dp(8), 0);
        titleRow.addView(look.text("Job Board", 34, 700, true, SaltLook.LABEL),
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ImageView plus = look.icon("ic_add", SaltLook.PURPLE, 24);
        look.press(plus, () -> open(postJobPage()));
        titleRow.addView(plus);
        body.addView(titleRow);
        EditText search = searchField("Search jobs");
        search.setText(jobQuery);
        body.addView(searchWrap(search));
        LinearLayout results = look.column();
        body.addView(results);
        refreshJobs(results);
        search.addTextChangedListener(watcher(value -> {
            jobQuery = value;
            refreshJobs(results);
        }));
        scroll.addView(body);
        return scroll;
    }

    private void refreshJobs(LinearLayout results) {
        results.removeAllViews();
        boolean any = false;
        for (SaltData.Job job : SaltData.jobs) {
            String haystack = (job.title + " " + job.company + " " + job.location).toLowerCase(Locale.US);
            if (!jobQuery.isEmpty() && !haystack.contains(jobQuery.toLowerCase(Locale.US))) continue;
            any = true;
            results.addView(jobRow(job));
        }
        if (!any) results.addView(empty("No Jobs Found", "Check back soon or post a job for free as a member."));
    }

    private View jobRow(SaltData.Job job) {
        LinearLayout row = look.column();
        row.setPadding(look.dp(16), look.dp(14), look.dp(16), look.dp(14));
        LinearLayout title = look.row(Gravity.CENTER_VERTICAL);
        TextView name = look.text(job.title, 17, 600, true, SaltLook.LABEL);
        name.setMaxLines(2);
        title.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (job.featured) title.addView(capsule("FEATURED", 9, android.graphics.Color.BLACK, SaltLook.GOLD, 6, 3));
        row.addView(title);
        look.add(row, look.text(job.company, 15, 400, false, SaltLook.PURPLE), 8);
        LinearLayout meta = look.row(Gravity.CENTER_VERTICAL);
        meta.addView(infoRow("ic_place", job.location, SaltLook.SECONDARY_LABEL, null));
        View clock = infoRow("ic_clock", job.type, SaltLook.SECONDARY_LABEL, null);
        LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clockParams.leftMargin = look.dp(12);
        meta.addView(clock, clockParams);
        look.add(row, meta, 8);
        LinearLayout footer = look.row(Gravity.CENTER_VERTICAL);
        if (job.salary != null) {
            footer.addView(look.text(job.salary, 12, 400, false, SaltLook.GOLD),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        } else {
            footer.addView(new View(activity), new LinearLayout.LayoutParams(0, 1, 1f));
        }
        footer.addView(look.text(SaltData.daysLabel(job.posted), 12, 400, false, SaltLook.SECONDARY_LABEL));
        look.add(row, footer, 8);
        look.press(row, () -> open(jobPage(job)));
        LinearLayout wrap = look.column();
        wrap.addView(row);
        wrap.addView(insetLine());
        return wrap;
    }

    private View jobPage(SaltData.Job job) {
        LinearLayout page = navPage(job.title);
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.setPadding(look.dp(16), look.dp(16), look.dp(16), look.dp(32));
        if (job.featured) body.addView(capsule("FEATURED", 10, android.graphics.Color.BLACK, SaltLook.GOLD, 8, 4));
        look.add(body, look.text(job.title, 28, 700, true, SaltLook.LABEL), 8);
        look.add(body, look.text(job.company, 20, 600, true, SaltLook.PURPLE), 8);
        LinearLayout meta = look.row(Gravity.CENTER_VERTICAL);
        meta.addView(infoRow("ic_place", job.location, SaltLook.SECONDARY_LABEL, null));
        View type = infoRow("ic_clock", job.type, SaltLook.SECONDARY_LABEL, null);
        LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        typeParams.leftMargin = look.dp(16);
        meta.addView(type, typeParams);
        look.add(body, meta, 8);
        if (job.salary != null) look.add(body, look.text(job.salary, 17, 600, true, SaltLook.GOLD), 8);
        look.add(body, look.text("Posted " + SaltData.daysLabel(job.posted), 12, 400, false, SaltLook.SECONDARY_LABEL), 8);
        TextView description = look.text(job.description, 17, 400, false, SaltLook.SECONDARY_LABEL);
        body.addView(cardBlock("Description", description));
        if (job.requirements.length > 0) {
            LinearLayout requirements = look.column();
            requirements.addView(look.text("Requirements", 17, 600, true, SaltLook.LABEL));
            for (String requirement : job.requirements) {
                look.add(requirements, infoRow("ic_check", requirement, SaltLook.SECONDARY_LABEL, null), 8);
            }
            body.addView(cardWrap(requirements));
        }
        TextView apply = look.button("Quick Apply", SaltLook.PURPLE, android.graphics.Color.WHITE);
        look.press(apply, () -> alert("Application Sent",
            "Your interest has been sent to " + job.company + ". They will contact you if there's a match. (Demo mode)"));
        look.add(body, apply, 20);
        scroll.addView(body);
        page.addView(scroll, match());
        return page;
    }

    private View morePage() {
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.addView(largeTitle("More"));
        body.addView(moreRow("ic_book", "Resources", this::openResources));
        body.addView(moreRow("ic_message", "Messages", () -> open(messagesPage())));
        body.addView(moreRow("ic_person", "Profile", () -> open(profilePage())));
        body.addView(moreRow("ic_heart", "Donate", () -> open(donatePage())));
        body.addView(moreRow("ic_doc", "Documents", () -> open(documentsPage())));
        body.addView(moreRow("ic_info", "About SALT", () -> open(aboutPage())));
        scroll.addView(body);
        return scroll;
    }

    private void openResources() {
        tab = 4;
        paintTabs();
        pages.clear();
        push(resourcesPage(), false);
    }

    private View resourcesPage() {
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.addView(largeTitle("Resources"));
        body.addView(chips(SaltData.RESOURCE_FILTERS, resourceFilter, value -> {
            resourceFilter = value;
            openResources();
        }));
        for (SaltData.Resource resource : SaltData.resources) {
            if (!"All".equals(resourceFilter) && !resource.category.equals(resourceFilter)) continue;
            body.addView(resourceRow(resource));
        }
        scroll.addView(body);
        return scroll;
    }

    private View resourceRow(SaltData.Resource resource) {
        LinearLayout row = look.row(Gravity.CENTER_VERTICAL);
        row.setPadding(look.dp(16), look.dp(14), look.dp(16), look.dp(14));
        row.addView(iconTile(resource.icon, SaltLook.PURPLE, 44, 10));
        LinearLayout text = look.column();
        LinearLayout title = look.row(Gravity.CENTER_VERTICAL);
        TextView name = look.text(resource.title, 17, 600, true, SaltLook.LABEL);
        name.setMaxLines(2);
        title.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (resource.membersOnly) title.addView(look.icon("ic_lock", SaltLook.GOLD, 12));
        text.addView(title);
        look.add(text, look.text(resource.category, 12, 400, false, SaltLook.SECONDARY_LABEL), 4);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.leftMargin = look.dp(14);
        row.addView(text, params);
        look.press(row, () -> open(resourcePage(resource)));
        LinearLayout wrap = look.column();
        wrap.addView(row);
        wrap.addView(insetLine());
        return wrap;
    }

    private View resourcePage(SaltData.Resource resource) {
        LinearLayout page = navPage(resource.title);
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.setPadding(look.dp(16), look.dp(16), look.dp(16), look.dp(32));
        LinearLayout header = look.row(Gravity.CENTER_VERTICAL);
        header.addView(iconTile(resource.icon, SaltLook.PURPLE, 56, 14));
        LinearLayout meta = look.column();
        TextView category = look.text(resource.category, 12, 400, false, SaltLook.SECONDARY_LABEL);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        metaParams.leftMargin = look.dp(12);
        meta.addView(category);
        if (resource.membersOnly) look.add(meta, look.text("Members Only", 12, 400, false, SaltLook.GOLD), 4);
        header.addView(meta, metaParams);
        body.addView(header);
        look.add(body, look.text(resource.title, 22, 600, true, SaltLook.LABEL), 20);
        look.add(body, look.text(resource.description, 17, 400, false, SaltLook.SECONDARY_LABEL), 12);
        TextView content = look.text(resource.content, 17, 400, false, SaltLook.LABEL);
        content.setPadding(look.dp(16), look.dp(16), look.dp(16), look.dp(16));
        content.setBackground(look.rounded(SaltLook.SECONDARY, 12));
        look.add(body, content, 20);
        TextView link = look.button("Open External Resource", SaltLook.PURPLE, android.graphics.Color.WHITE);
        look.press(link, () -> browse(resource.url));
        look.add(body, link, 20);
        look.add(body, look.text("Last updated " + SaltData.medium(System.currentTimeMillis()), 12, 400, false, SaltLook.SECONDARY_LABEL), 12);
        scroll.addView(body);
        page.addView(scroll, match());
        return page;
    }

    private View messagesPage() {
        LinearLayout page = navPage("Messages");
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.setPadding(look.dp(16), look.dp(8), look.dp(16), look.dp(24));
        body.addView(look.text("Group chats", 17, 600, true, SaltLook.LABEL));
        EditText key = look.input("Encryption key", true);
        look.add(body, key, 12);
        TextView status = look.text("", 13, 400, false, SaltLook.SECONDARY_LABEL);
        TextView create = look.button("Create encrypted group", SaltLook.alpha(SaltLook.PURPLE, 0.12f), SaltLook.PURPLE);
        look.press(create, () -> createGroup(key.getText().toString(), status));
        look.add(body, create, 12);
        for (GroupChat chat : groups) body.addView(groupCard(chat, key, status));
        look.add(body, status, 8);
        for (SaltData.Member member : SaltData.members) {
            if (member.id.equals(SaltData.current.id)) continue;
            LinearLayout row = look.column();
            row.setPadding(0, look.dp(12), 0, look.dp(12));
            row.addView(look.text(member.displayName(), 17, 600, true, SaltLook.LABEL));
            look.add(row, look.text("Start a conversation with " + member.displayName() + ".", 15, 400, false, SaltLook.SECONDARY_LABEL), 4);
            look.press(row, () -> open(chatPage(member)));
            look.add(body, row, 8);
            body.addView(insetLine());
        }
        scroll.addView(body);
        page.addView(scroll, match());
        return page;
    }

    private View groupCard(GroupChat chat, EditText key, TextView status) {
        LinearLayout card = look.column();
        card.addView(look.text(chat.id.substring(0, 8) + "…", 15, 400, false, SaltLook.LABEL));
        look.add(card, look.text(chat.blocked ? "Access blocked" : "Members: " + chat.members.size(), 12, 400, false, SaltLook.SECONDARY_LABEL), 6);
        TextView unlock = look.text("Unlock", 17, 600, true, SaltLook.PURPLE);
        look.press(unlock, () -> unlockGroup(chat, key.getText().toString(), status));
        look.add(card, unlock, 6);
        EditText member = look.input("Member id", false);
        member.setInputType(InputType.TYPE_CLASS_TEXT);
        look.add(card, member, 6);
        if (chat.creatorId.equals(SaltData.current.id)) {
            TextView approve = look.text("Approve member", 15, 400, false, SaltLook.PURPLE);
            TextView remove = look.text("Remove member", 15, 400, false, SaltLook.PURPLE);
            look.press(approve, () -> decide(chat, member.getText().toString(), true, status));
            look.press(remove, () -> decide(chat, member.getText().toString(), false, status));
            look.add(card, approve, 6);
            look.add(card, remove, 6);
        } else {
            TextView request = look.text("Request to add member", 15, 400, false, SaltLook.PURPLE);
            look.press(request, () -> {
                status.setText("Only the chat creator can approve a member.");
            });
            look.add(card, request, 6);
        }
        return card;
    }

    private void createGroup(String passphrase, TextView status) {
        if (passphrase.length() < 12) {
            status.setText("Use a key of at least 12 characters.");
            return;
        }
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] derived = SaltCrypto.chatKey(passphrase, salt);
        GroupChat chat = new GroupChat();
        chat.salt = salt;
        chat.wrapped = SaltCrypto.seal(new byte[] {1, 2, 3, 4}, derived);
        chat.policy.wrappedKey = chat.wrapped;
        chat.creatorId = SaltData.current.id;
        chat.members.add(SaltData.current.id);
        groups.add(0, chat);
        status.setText("Group created. Only this key can open it.");
        open(messagesPage());
        pages.remove(pages.size() - 2);
    }

    private void unlockGroup(GroupChat chat, String passphrase, TextView status) {
        if (chat.blocked || passphrase.length() < 12) {
            status.setText(chat.blocked ? "The key was removed and access to this chat is blocked." : "Use a key of at least 12 characters.");
            return;
        }
        if (chat.checking) return;
        chat.checking = true;
        status.setText("Checking the key…");
        String attempt = passphrase;
        new Thread(() -> {
            boolean opened = false;
            try {
                SaltCrypto.open(chat.wrapped, SaltCrypto.chatKey(attempt, chat.salt));
                opened = true;
            } catch (SecurityException ex) {
                opened = false;
            }
            boolean matched = opened;
            activity.runOnUiThread(() -> {
                chat.checking = false;
                KeyAttemptPolicy.Result result = chat.policy.attempt(matched, System.currentTimeMillis(), SaltData.current.id, userId -> {
                    chat.members.remove(userId);
                    chat.blocked = true;
                    chat.wrapped = "";
                });
                if (status.getParent() == null) return;
                if (result == KeyAttemptPolicy.Result.UNLOCKED) status.setText("Unlocked on this device. Messages stay encrypted.");
                else if (result == KeyAttemptPolicy.Result.DELAYED) status.setText("Too many attempts. Try again after " + shortTime(chat.policy.lockedUntilMillis) + ".");
                else if (result == KeyAttemptPolicy.Result.BLOCKED) status.setText("The key was removed and access to this chat is blocked.");
                else status.setText("Key rejected. After three failures there is a 15 minute delay, then two final attempts.");
            });
        }, "salt-group-key").start();
    }

    private void decide(GroupChat chat, String memberId, boolean approve, TextView status) {
        if (!chat.creatorId.equals(SaltData.current.id)) return;
        if (approve && !memberId.isEmpty() && !chat.members.contains(memberId)) chat.members.add(memberId);
        if (!approve) chat.members.remove(memberId);
        status.setText(approve ? "Member approved by the chat creator." : "Member removed by the chat creator.");
        open(messagesPage());
        pages.remove(pages.size() - 2);
    }

    private View chatPage(SaltData.Member member) {
        DirectChat chat = direct(member.id);
        LinearLayout page = navPage(member.displayName());
        ScrollView scroll = scroll();
        LinearLayout transcript = look.column();
        transcript.setPadding(look.dp(16), look.dp(16), look.dp(16), look.dp(16));
        for (String message : chat.messages) {
            TextView bubble = look.text(displayMessage(chat, message), 17, 400, false, SaltLook.LABEL);
            bubble.setBackground(look.rounded(SaltLook.SECONDARY, 12));
            bubble.setPadding(look.dp(12), look.dp(12), look.dp(12), look.dp(12));
            look.add(transcript, bubble, 12);
        }
        scroll.addView(transcript);
        page.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        page.addView(look.hairline());
        LinearLayout composer = look.column();
        composer.setPadding(look.dp(16), look.dp(8), look.dp(16), look.dp(12));
        EditText passphrase = look.input("Encryption key", true);
        composer.addView(passphrase);
        LinearLayout unlockRow = look.row(Gravity.CENTER_VERTICAL);
        TextView unlock = look.text("Unlock", 17, 600, true, SaltLook.PURPLE);
        TextView status = look.text(directStatus(chat), 12, 400, false, SaltLook.SECONDARY_LABEL);
        unlockRow.addView(unlock);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        statusParams.leftMargin = look.dp(12);
        unlockRow.addView(status, statusParams);
        look.add(composer, unlockRow, 8);
        LinearLayout sendRow = look.row(Gravity.CENTER_VERTICAL);
        EditText draft = look.input("Message", false);
        draft.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        sendRow.addView(draft, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ImageView send = look.icon("ic_send", SaltLook.PURPLE, 24);
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(look.dp(24), look.dp(24));
        sendParams.leftMargin = look.dp(8);
        sendRow.addView(send, sendParams);
        look.add(composer, sendRow, 8);
        look.press(unlock, () -> unlockDirect(chat, passphrase.getText().toString(), status, member));
        look.press(send, () -> sendDirect(chat, draft.getText().toString(), member));
        page.addView(composer);
        return page;
    }

    private void unlockDirect(DirectChat chat, String passphrase, TextView status, SaltData.Member member) {
        if (passphrase.length() < 12) {
            status.setText("Use a key of at least 12 characters.");
            return;
        }
        if (chat.checking) return;
        chat.checking = true;
        status.setText("Checking the key…");
        String attempt = passphrase;
        new Thread(() -> {
            byte[] derived = SaltCrypto.chatKey(attempt, chat.salt);
            boolean first = chat.wrapped == null;
            boolean matched = first;
            if (!first) {
                try {
                    SaltCrypto.open(chat.wrapped, derived);
                    matched = true;
                } catch (SecurityException ex) {
                    matched = false;
                }
            }
            boolean opened = matched;
            activity.runOnUiThread(() -> finishUnlock(chat, member, derived, first, opened));
        }, "salt-direct-key").start();
    }

    private void finishUnlock(DirectChat chat, SaltData.Member member, byte[] derived, boolean first, boolean matched) {
        chat.checking = false;
        if (first) {
            chat.wrapped = SaltCrypto.seal(new byte[] {9, 9, 9, 9}, derived);
            chat.policy.wrappedKey = chat.wrapped;
            chat.sessionKey = derived;
            chat.unlocked = true;
            showDirect(chat, member, "Unlocked on this device.");
            return;
        }
        KeyAttemptPolicy.Result result = chat.policy.attempt(matched, System.currentTimeMillis(), SaltData.current.id, userId -> {
            chat.blocked = true;
            chat.wrapped = "";
            chat.unlocked = false;
            chat.sessionKey = null;
        });
        chat.unlocked = result == KeyAttemptPolicy.Result.UNLOCKED;
        chat.sessionKey = chat.unlocked ? derived : null;
        String notice;
        if (chat.unlocked) notice = "Unlocked on this device.";
        else if (result == KeyAttemptPolicy.Result.DELAYED) notice = "Too many attempts. Try again after " + shortTime(chat.policy.lockedUntilMillis) + ".";
        else if (result == KeyAttemptPolicy.Result.BLOCKED) notice = "The key was removed and access is blocked.";
        else notice = "Key rejected. After three failures there is a 15 minute delay, then two final attempts.";
        showDirect(chat, member, notice);
    }

    private void sendDirect(DirectChat chat, String text, SaltData.Member member) {
        String trimmed = text.trim();
        if (chat.blocked) {
            showDirect(chat, member, "The key was removed and access is blocked.");
            return;
        }
        if (!chat.unlocked || chat.sessionKey == null) {
            showDirect(chat, member, "Unlock this chat before sending.");
            return;
        }
        if (trimmed.isEmpty()) return;
        try {
            chat.messages.add(SaltCrypto.seal(trimmed.getBytes(java.nio.charset.StandardCharsets.UTF_8), chat.sessionKey));
        } catch (SecurityException ex) {
            showDirect(chat, member, "The message could not be encrypted.");
            return;
        }
        showDirect(chat, member, "Unlocked on this device.");
    }

    private String directStatus(DirectChat chat) {
        if (chat.notice != null) return chat.notice;
        if (chat.blocked) return "The key was removed and access is blocked.";
        if (chat.unlocked) return "Unlocked on this device.";
        return "Enter the key from the person who started this chat.";
    }

    private void showDirect(DirectChat chat, SaltData.Member member, String notice) {
        chat.notice = notice;
        open(chatPage(member));
        pages.remove(pages.size() - 2);
    }

    private String displayMessage(DirectChat chat, String ciphertext) {
        if (!chat.unlocked || chat.sessionKey == null) return "Encrypted message";
        try {
            return new String(SaltCrypto.open(ciphertext, chat.sessionKey), java.nio.charset.StandardCharsets.UTF_8);
        } catch (SecurityException ex) {
            return "Encrypted message";
        }
    }

    private final java.util.Map<String, DirectChat> directs = new java.util.HashMap<>();

    private DirectChat direct(String memberId) {
        String id = SaltData.current.id.compareTo(memberId) < 0 ? SaltData.current.id + "-" + memberId : memberId + "-" + SaltData.current.id;
        DirectChat chat = directs.get(id);
        if (chat == null) {
            chat = new DirectChat();
            new SecureRandom().nextBytes(chat.salt);
            directs.put(id, chat);
        }
        return chat;
    }

    private View profilePage() {
        LinearLayout page = navPage("Profile");
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        LinearLayout header = look.row(Gravity.CENTER_VERTICAL);
        header.setPadding(look.dp(16), look.dp(16), look.dp(16), look.dp(16));
        header.setBackgroundColor(SaltLook.SECONDARY);
        header.addView(avatar(SaltData.current.displayName(), 72, 28));
        LinearLayout names = look.column();
        names.addView(look.text(SaltData.current.displayName(), 20, 600, true, SaltLook.LABEL));
        look.add(names, look.text(SaltData.current.email, 12, 400, false, SaltLook.SECONDARY_LABEL), 4);
        LinearLayout badges = look.row(Gravity.CENTER_VERTICAL);
        int statusColor = "active".equals(SaltData.current.status) ? SaltLook.GREEN : SaltLook.GOLD;
        badges.addView(capsule(capitalize(SaltData.current.status), 12, statusColor, SaltLook.alpha(statusColor, 0.15f), 8, 3));
        TextView tier = look.text(SaltData.current.tier.contains("Business") ? "Business" : "Individual", 12, 400, false, SaltLook.PURPLE);
        LinearLayout.LayoutParams tierParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tierParams.leftMargin = look.dp(6);
        badges.addView(tier, tierParams);
        if (SaltData.current.board) {
            TextView admin = capsule("Admin", 12, android.graphics.Color.WHITE, SaltLook.PURPLE, 8, 3);
            LinearLayout.LayoutParams adminParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            adminParams.leftMargin = look.dp(6);
            badges.addView(admin, adminParams);
        }
        look.add(names, badges, 6);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameParams.leftMargin = look.dp(16);
        header.addView(names, nameParams);
        body.addView(header);
        body.addView(sectionLabel("Membership"));
        body.addView(settingRow("Tier", SaltData.current.tier, null));
        body.addView(settingRow("Annual Dues", "$" + SaltData.current.dues, null));
        body.addView(settingRow("Member Since", SaltData.medium(SaltData.current.joined), null));
        body.addView(settingRow("Renew / Pay Dues", "", () -> open(duesPage())));
        body.addView(settingRow("Apply for Membership", "", () -> open(formPage("Apply for Membership", membershipForm()))));
        body.addView(sectionLabel("Account"));
        body.addView(settingRow("Messages", "", () -> open(messagesPage())));
        body.addView(settingRow("Resources", "", this::openResources));
        body.addView(settingRow("Edit Profile", "", () -> open(notePage("Edit Profile", "Edit Profile — connect to Firestore update"))));
        body.addView(settingRow("Notifications", "", () -> open(notePage("Notifications", "Notification settings"))));
        if (SaltData.current.board) {
            body.addView(sectionLabel("Admin"));
            body.addView(settingRow("Admin Tools", "", () -> open(adminPage())));
            body.addView(settingRow("Pending Applications", "", () -> open(notePage("Pending Applications", "No pending applications in this demo."))));
        }
        body.addView(sectionLabel("Support & About"));
        body.addView(settingRow("Donate", "", () -> open(donatePage())));
        body.addView(settingRow("Member Documents", "", () -> open(documentsPage())));
        body.addView(settingRow("About SALT", "", () -> open(aboutPage())));
        TextView signOut = look.text("Sign Out", 17, 400, false, SaltLook.RED);
        signOut.setPadding(look.dp(16), look.dp(16), look.dp(16), look.dp(16));
        look.press(signOut, () -> new AlertDialog.Builder(activity)
            .setTitle("Sign Out?")
            .setPositiveButton("Sign Out", (dialog, which) -> showLogin())
            .setNegativeButton("Cancel", null)
            .show());
        body.addView(signOut);
        scroll.addView(body);
        page.addView(scroll, match());
        return page;
    }

    private View duesPage() {
        LinearLayout page = navPage("Membership Dues");
        LinearLayout body = look.column();
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.setPadding(look.dp(24), look.dp(48), look.dp(24), look.dp(24));
        ImageView card = look.icon("ic_card", SaltLook.GOLD, 48);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(look.dp(48), look.dp(48));
        iconParams.gravity = Gravity.CENTER_HORIZONTAL;
        body.addView(card, iconParams);
        TextView title = look.text("Membership Dues", 22, 600, true, SaltLook.LABEL);
        title.setGravity(Gravity.CENTER);
        look.add(body, title, 24);
        TextView tier = look.text(SaltData.current.tier, 17, 600, true, SaltLook.LABEL);
        tier.setGravity(Gravity.CENTER);
        look.add(body, tier, 8);
        TextView amount = look.text("$" + SaltData.current.dues + " / year", 28, 700, true, SaltLook.GOLD);
        amount.setGravity(Gravity.CENTER);
        look.add(body, amount, 8);
        TextView note = look.text("Supports advocacy, networking events, and member resources.", 15, 400, false, SaltLook.SECONDARY_LABEL);
        note.setGravity(Gravity.CENTER);
        look.add(body, note, 8);
        page.addView(body);
        return page;
    }

    private View adminPage() {
        LinearLayout page = navPage("Admin Tools");
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.addView(sectionLabel("Posts"));
        for (SaltData.Post post : new ArrayList<>(SaltData.posts)) {
            LinearLayout row = look.column();
            row.setPadding(look.dp(16), look.dp(12), look.dp(16), look.dp(12));
            row.addView(look.text(post.author, 12, 400, false, SaltLook.SECONDARY_LABEL));
            TextView content = look.text(post.content, 17, 400, false, SaltLook.LABEL);
            content.setMaxLines(3);
            look.add(row, content, 6);
            TextView delete = look.text("Delete", 15, 600, true, SaltLook.RED);
            look.press(delete, () -> {
                SaltData.posts.remove(post);
                open(adminPage());
                pages.remove(pages.size() - 2);
            });
            look.add(row, delete, 6);
            body.addView(row);
            body.addView(insetLine());
        }
        scroll.addView(body);
        page.addView(scroll, match());
        return page;
    }

    private View donatePage() {
        LinearLayout page = navPage("Donate");
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.setPadding(look.dp(24), look.dp(12), look.dp(24), look.dp(32));
        ImageView logo = new ImageView(activity);
        logo.setImageResource(R.drawable.salt_logo);
        logo.setAdjustViewBounds(true);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(look.dp(150), LinearLayout.LayoutParams.WRAP_CONTENT);
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        body.addView(logo, logoParams);
        look.add(body, look.text("Support SALT", 22, 600, true, SaltLook.PURPLE), 20);
        look.add(body, look.text("Your donation helps support local tourism advocacy, professional standards, civic engagement, and community initiatives across Savannah.", 17, 400, false, SaltLook.SECONDARY_LABEL), 8);
        look.add(body, look.text("Donation Amount", 17, 600, true, SaltLook.LABEL), 20);
        int[] amounts = {25, 50, 100, 250};
        final int[] selected = {50};
        LinearLayout grid = look.row(Gravity.CENTER);
        TextView[] choices = new TextView[amounts.length];
        for (int i = 0; i < amounts.length; i++) {
            int amount = amounts[i];
            TextView choice = look.text("$" + amount, 17, 600, true, amount == 50 ? android.graphics.Color.WHITE : SaltLook.LABEL);
            choice.setGravity(Gravity.CENTER);
            choice.setBackground(look.rounded(amount == 50 ? SaltLook.PURPLE : SaltLook.SECONDARY, 12));
            choice.setPadding(0, look.dp(14), 0, look.dp(14));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            params.leftMargin = i == 0 ? 0 : look.dp(12);
            grid.addView(choice, params);
            choices[i] = choice;
            int index = i;
            look.press(choice, () -> {
                selected[0] = amount;
                for (int j = 0; j < choices.length; j++) {
                    boolean on = j == index;
                    choices[j].setBackground(look.rounded(on ? SaltLook.PURPLE : SaltLook.SECONDARY, 12));
                    choices[j].setTextColor(on ? android.graphics.Color.WHITE : SaltLook.LABEL);
                }
            });
        }
        look.add(body, grid, 12);
        TextView donate = look.button("Donate Now", SaltLook.PURPLE, android.graphics.Color.WHITE);
        look.press(donate, () -> browse("https://www.zeffy.com/en-US/fundraising/salt-donation"));
        look.add(body, donate, 20);
        look.add(body, look.text("Donations are processed securely through SALT's official Zeffy fundraiser. 100% of your donation goes to SALT.", 12, 400, false, SaltLook.SECONDARY_LABEL), 12);
        scroll.addView(body);
        page.addView(scroll, match());
        return page;
    }

    private View documentsPage() {
        LinearLayout page = navPage("Member Documents");
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.setPadding(look.dp(16), look.dp(8), look.dp(16), look.dp(24));
        body.addView(documentCard("SALT Code of Conduct", "Professional standards and expectations",
            "Members agree to follow applicable laws, practice honesty and transparency, treat guests, residents, officials, workers, and fellow members with respect, avoid discrimination, prioritize safety, respect public and private property, resolve disputes professionally, maintain good standing, and report Code of Conduct violations to the SALT Board.",
            "https://saltsavannah.org/wp-content/uploads/2025/04/SALT-Code-of-Conduct2.pdf", "ic_shield"));
        look.add(body, documentCard("SALT Bylaws", "Governance, membership, voting, and committees",
            "SALT is organized as a Georgia nonprofit 501(c)(6) business league. The bylaws define SALT's purpose, membership eligibility and categories, application review by the Board, voting rights for Active Members, dues, termination, member and board meetings, officer roles, and standing committees including Ghostess City, Halloween Parade, and MOSS.",
            "https://saltsavannah.org/wp-content/uploads/2026/02/WEB-LINK-SALT-ByLaws-w_Committees.pdf", "ic_doc"), 16);
        look.add(body, look.text("Official documents are hosted by SALT. Use the PDF links for the full current text.", 12, 400, false, SaltLook.SECONDARY_LABEL), 16);
        scroll.addView(body);
        page.addView(scroll, match());
        return page;
    }

    private View documentCard(String title, String subtitle, String summary, String url, String icon) {
        LinearLayout card = look.column();
        card.setPadding(look.dp(4), look.dp(8), look.dp(4), look.dp(8));
        LinearLayout header = look.row(Gravity.TOP);
        header.addView(iconTile(icon, SaltLook.PURPLE, 44, 10));
        LinearLayout text = look.column();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.leftMargin = look.dp(12);
        text.addView(look.text(title, 17, 600, true, SaltLook.LABEL));
        look.add(text, look.text(subtitle, 12, 400, false, SaltLook.SECONDARY_LABEL), 4);
        header.addView(text, params);
        card.addView(header);
        look.add(card, look.text(summary, 17, 400, false, SaltLook.LABEL), 12);
        TextView link = look.button("View Official PDF", SaltLook.PURPLE, android.graphics.Color.WHITE);
        link.setPadding(look.dp(16), look.dp(12), look.dp(16), look.dp(12));
        look.press(link, () -> browse(url));
        look.add(card, link, 12);
        return card;
    }

    private View aboutPage() {
        LinearLayout page = navPage("About SALT");
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.setPadding(look.dp(24), look.dp(12), look.dp(24), look.dp(32));
        ImageView logo = new ImageView(activity);
        logo.setImageResource(R.drawable.salt_logo);
        logo.setAdjustViewBounds(true);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(look.dp(180), LinearLayout.LayoutParams.WRAP_CONTENT);
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        body.addView(logo, logoParams);
        TextView gold = look.text("Strengthening Savannah's Local Tourism", 20, 600, true, SaltLook.GOLD);
        gold.setLineSpacing(look.dp(4), 1f);
        look.add(body, gold, 18);
        String[] paragraphs = {
            "The Savannah Association of Local Tours (SALT) was founded in 2025 by locally owned tour operators who share a deep commitment to the city we call home. Created at a pivotal moment for Savannah’s tourism industry, SALT unified local professionals to ensure that small business voices were represented while supporting thoughtful, responsible growth.",
            "Today, SALT works to strengthen the local tourism environment through collaboration, professional standards, and constructive civic engagement. Our members believe that a thriving visitor economy and a vibrant community go hand in hand — and that those who benefit from Savannah share a responsibility to help care for it.",
            "From industry coordination to hands-on initiatives like Maintaining Our Savannah Squares (M.O.S.S.) and the development of family-friendly cultural events, SALT is dedicated to supporting a city where residents, businesses, and visitors alike can flourish.",
            "We are proud to invest in Savannah — not just as a destination, but as a community."
        };
        for (String paragraph : paragraphs) {
            TextView copy = look.text(paragraph, 17, 400, false, SaltLook.LABEL);
            copy.setLineSpacing(look.dp(4), 1f);
            look.add(body, copy, 18);
        }
        scroll.addView(body);
        page.addView(scroll, match());
        return page;
    }

    private View composerPage() {
        LinearLayout page = navPage("New Post");
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.setPadding(look.dp(16), look.dp(16), look.dp(16), look.dp(24));
        EditText content = look.input("Share something with members", false);
        content.setSingleLine(false);
        content.setMinLines(6);
        content.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        content.setGravity(Gravity.TOP);
        body.addView(content);
        HorizontalScrollView emojis = new HorizontalScrollView(activity);
        emojis.setHorizontalScrollBarEnabled(false);
        LinearLayout emojiRow = look.row(Gravity.CENTER_VERTICAL);
        for (String emoji : new String[] {"😀", "👏", "🎉", "📣", "⭐️", "❤️", "💬", "📍"}) {
            TextView item = look.text(emoji, 22, 400, false, SaltLook.LABEL);
            item.setGravity(Gravity.CENTER);
            item.setBackground(look.rounded(SaltLook.SECONDARY, 10));
            item.setWidth(look.dp(40));
            item.setHeight(look.dp(40));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(look.dp(40), look.dp(40));
            params.rightMargin = look.dp(8);
            emojiRow.addView(item, params);
            look.press(item, () -> content.append(emoji));
        }
        emojis.addView(emojiRow);
        look.add(body, emojis, 16);
        TextView photo = look.button("Add Images or Videos", SaltLook.alpha(SaltLook.PURPLE, 0.12f), SaltLook.PURPLE);
        look.press(photo, () -> activity.startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), PHOTO_REQUEST));
        look.add(body, photo, 16);
        composerStatus = look.text("Videos are not uploaded. Use a still image so faces can be blurred first.", 13, 400, false, SaltLook.SECONDARY_LABEL);
        look.add(body, composerStatus, 8);
        composerPreview = new ImageView(activity);
        composerPreview.setVisibility(View.GONE);
        composerPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(look.dp(72), look.dp(72));
        previewParams.topMargin = look.dp(12);
        body.addView(composerPreview, previewParams);
        clip(composerPreview, 12);
        TextView consent = look.button("Send faces to admin for consent review", SaltLook.alpha(SaltLook.PURPLE, 0.12f), SaltLook.PURPLE);
        consent.setVisibility(View.GONE);
        look.press(consent, () -> sendConsent());
        look.add(body, consent, 12);
        composerPreview.addOnLayoutChangeListener((v, a, b, c, d, e, f, g, h) -> consent.setVisibility(facesFound ? View.VISIBLE : View.GONE));
        TextView post = look.button("Post", SaltLook.PURPLE, android.graphics.Color.WHITE);
        look.press(post, () -> publish(content.getText().toString()));
        look.add(body, post, 16);
        scroll.addView(body);
        page.addView(scroll, match());
        return page;
    }

    private void sendConsent() {
        if (pendingOriginal == null || !facesFound) return;
        try {
            FaceBlur.Prepared prepared = FaceBlur.prepare(activity, pendingOriginal, true);
            Arrays.fill(pendingOriginal, (byte) 0);
            pendingOriginal = null;
            if (prepared.publicJpeg != null) publicPhoto = prepared.publicJpeg;
            composerStatus.setText(prepared.reviewId != null
                ? "Encrypted review sent. The public post keeps the blurred photo."
                : "The unblurred photo was discarded.");
        } catch (SecurityException ex) {
            discardPhoto();
            composerStatus.setText("The photo was not uploaded because faces could not be scanned.");
        }
    }

    private void publish(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty() && publicPhoto == null) {
            composerStatus.setText("Add text or a blurred photo.");
            return;
        }
        try {
            postLimiter.consume(SaltData.current.id, System.currentTimeMillis());
        } catch (SecurityException ex) {
            composerStatus.setText("Public posts are limited to 3 per hour.");
            return;
        }
        SaltData.Post post = new SaltData.Post("local-" + System.currentTimeMillis(), SaltData.current.fullName(),
            SaltData.current.business, trimmed.isEmpty() ? "Shared a photo." : trimmed, System.currentTimeMillis(),
            0, 0, false, false, false);
        post.image = publicPhoto;
        publicPhoto = null;
        discardPhoto();
        SaltData.posts.add(0, post);
        showTab(0);
    }

    private View commentsPage(SaltData.Post post) {
        LinearLayout page = navPage("Comments");
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.setPadding(look.dp(16), look.dp(16), look.dp(16), look.dp(16));
        body.addView(postCard(post));
        look.add(body, look.text("Comments", 17, 600, true, SaltLook.LABEL), 16);
        boolean any = false;
        for (SaltData.Comment comment : SaltData.comments) {
            if (!comment.postId.equals(post.id)) continue;
            any = true;
            LinearLayout card = look.column();
            card.setPadding(look.dp(16), look.dp(16), look.dp(16), look.dp(16));
            card.setBackground(look.rounded(SaltLook.SECONDARY, 12));
            LinearLayout header = look.row(Gravity.CENTER_VERTICAL);
            header.addView(look.text(comment.author, 17, 600, true, SaltLook.LABEL),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            header.addView(look.text(SaltData.ago(comment.created), 12, 400, false, SaltLook.SECONDARY_LABEL));
            card.addView(header);
            look.add(card, look.text(comment.content, 17, 400, false, SaltLook.LABEL), 6);
            look.add(body, card, 12);
        }
        if (!any) body.addView(empty("No Comments Yet", ""));
        scroll.addView(body);
        page.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        page.addView(look.hairline());
        LinearLayout composer = look.row(Gravity.CENTER_VERTICAL);
        composer.setPadding(look.dp(16), look.dp(8), look.dp(16), look.dp(12));
        EditText draft = look.input("Add a comment", false);
        draft.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        composer.addView(draft, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ImageView send = look.icon("ic_send", SaltLook.PURPLE, 22);
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(look.dp(22), look.dp(22));
        sendParams.leftMargin = look.dp(8);
        composer.addView(send, sendParams);
        look.press(send, () -> {
            String value = draft.getText().toString().trim();
            if (value.isEmpty()) return;
            SaltData.comments.add(new SaltData.Comment(post.id, SaltData.current.fullName(), value, System.currentTimeMillis()));
            post.comments += 1;
            open(commentsPage(post));
            pages.remove(pages.size() - 2);
        });
        page.addView(composer);
        return page;
    }

    private View createAccountForm() {
        LinearLayout body = look.column();
        body.addView(look.text("Create Account", 22, 600, true, SaltLook.LABEL));
        EditText email = look.input("Email", false);
        EditText phone = look.input("Phone", false);
        phone.setInputType(InputType.TYPE_CLASS_PHONE);
        EditText password = look.input("Strong Password", true);
        EditText confirm = look.input("Confirm Password", true);
        look.add(body, field("Email", email), 16);
        look.add(body, field("Phone", phone), 16);
        look.add(body, field("Strong Password", password), 16);
        look.add(body, field("Confirm Password", confirm), 16);
        TextView rules = look.text("", 13, 400, false, SaltLook.SECONDARY_LABEL);
        look.add(body, rules, 8);
        TextView error = look.text("", 13, 400, false, SaltLook.RED);
        error.setVisibility(View.GONE);
        TextWatcher watch = watcher(value -> rules.setText(passwordRules(password.getText().toString(), confirm.getText().toString())));
        password.addTextChangedListener(watch);
        confirm.addTextChangedListener(watch);
        final boolean[] sent = {false};
        EditText emailCode = look.input("Email Verification Code", false);
        EditText phoneCode = look.input("Phone Verification Code", false);
        emailCode.setInputType(InputType.TYPE_CLASS_NUMBER);
        phoneCode.setInputType(InputType.TYPE_CLASS_NUMBER);
        TextView hint = look.text("Demo verification code: 123456", 12, 400, false, SaltLook.SECONDARY_LABEL);
        hint.setVisibility(View.GONE);
        emailCode.setVisibility(View.GONE);
        phoneCode.setVisibility(View.GONE);
        TextView send = look.button("Send Verification Codes", SaltLook.alpha(SaltLook.PURPLE, 0.12f), SaltLook.PURPLE);
        look.press(send, () -> {
            if (!canVerify(email.getText().toString(), phone.getText().toString(), password.getText().toString(), confirm.getText().toString())) {
                error.setText("Use a valid email and a password with at least 12 characters, uppercase and lowercase letters, a number, and a symbol.");
                error.setVisibility(View.VISIBLE);
                return;
            }
            sent[0] = true;
            hint.setVisibility(View.VISIBLE);
            emailCode.setVisibility(View.VISIBLE);
            phoneCode.setVisibility(View.VISIBLE);
            error.setVisibility(View.GONE);
        });
        look.add(body, send, 16);
        look.add(body, hint, 12);
        look.add(body, field("Email Verification Code", emailCode), 12);
        look.add(body, field("Phone Verification Code", phoneCode), 16);
        look.add(body, error, 8);
        TextView create = look.button("Create Account", SaltLook.PURPLE, android.graphics.Color.WHITE);
        look.press(create, () -> {
            if (!sent[0] || !"123456".equals(emailCode.getText().toString()) || !"123456".equals(phoneCode.getText().toString())) {
                error.setText("Verify both email and phone before creating your account.");
                error.setVisibility(View.VISIBLE);
                return;
            }
            String local = email.getText().toString().split("@")[0];
            SaltData.current = new SaltData.Member("new-" + System.currentTimeMillis(), email.getText().toString(),
                capitalize(local), "", null, "Individual / Sole Proprietor", 150, "pending", System.currentTimeMillis(),
                phone.getText().toString(), null, "New SALT applicant.", false, "Other Tourism");
            enterApp();
        });
        look.add(body, create, 8);
        return body;
    }

    private View membershipForm() {
        LinearLayout body = look.column();
        body.addView(look.text("Individual / Sole Proprietor", 17, 600, true, SaltLook.LABEL));
        look.add(body, look.text("For independent guides, sole proprietors, and freelancers.", 15, 400, false, SaltLook.SECONDARY_LABEL), 4);
        look.add(body, look.text("$150 / year", 15, 600, true, SaltLook.GOLD), 4);
        look.add(body, look.text("For-Profit Business", 17, 600, true, SaltLook.LABEL), 16);
        look.add(body, look.text("For tour companies, restaurants, hotels, retail, and other tourism businesses.", 15, 400, false, SaltLook.SECONDARY_LABEL), 4);
        look.add(body, look.text("$350 / year", 15, 600, true, SaltLook.GOLD), 4);
        String[] labels = {"First", "Last", "Business / Organization", "Email", "Applicant's URL", "Phone", "Address Line 1", "Address Line 2", "City", "Zip Code", "Website / URL", "Brief Description of Services", "Number of Employees"};
        for (String label : labels) look.add(body, field(label, look.input(label, false)), 16);
        TextView affirm = look.text("I hereby apply for membership in the Savannah Association of Local Tours, Inc. (SALT). I affirm that the information provided in this application is true and accurate to the best of my knowledge. I agree to support the mission and objectives of SALT and comply with all bylaws, policies, and the Code of Conduct.", 13, 400, false, SaltLook.SECONDARY_LABEL);
        look.add(body, affirm, 16);
        look.add(body, field("Card", look.input("Card", false)), 16);
        look.add(body, field("Name on Card", look.input("Name on Card", false)), 16);
        look.add(body, field("MM/YY", look.input("MM/YY", false)), 16);
        look.add(body, field("CVV", look.input("CVV", true)), 16);
        look.add(body, field("Billing Zip Code", look.input("Billing Zip Code", false)), 16);
        TextView submit = look.button("Submit Application", SaltLook.PURPLE, android.graphics.Color.WHITE);
        look.press(submit, () -> alert("Application Received", "Your membership application has been submitted for board review."));
        look.add(body, submit, 20);
        return body;
    }

    private View postJobPage() {
        LinearLayout page = navPage("Post a Job");
        ScrollView scroll = scroll();
        LinearLayout body = look.column();
        body.setPadding(look.dp(16), look.dp(16), look.dp(16), look.dp(24));
        EditText title = look.input("Job Title", false);
        EditText company = look.input("Company", false);
        company.setText(SaltData.current.business == null ? "" : SaltData.current.business);
        EditText location = look.input("Location", false);
        location.setText("Savannah, GA");
        EditText description = look.input("Description", false);
        description.setSingleLine(false);
        description.setMinLines(4);
        EditText salary = look.input("Salary Range (optional)", false);
        look.add(body, field("Job Title", title), 0);
        look.add(body, field("Company", company), 16);
        look.add(body, field("Location", location), 16);
        look.add(body, field("Description", description), 16);
        look.add(body, field("Salary Range (optional)", salary), 16);
        look.add(body, look.text("Free for active SALT members. Non-members will be charged for postings.", 12, 400, false, SaltLook.SECONDARY_LABEL), 12);
        TextView post = look.button("Post", SaltLook.PURPLE, android.graphics.Color.WHITE);
        look.press(post, () -> {
            if (title.getText().toString().trim().isEmpty() || company.getText().toString().trim().isEmpty() || description.getText().toString().trim().isEmpty()) return;
            SaltData.jobs.add(0, new SaltData.Job(title.getText().toString().trim(), company.getText().toString().trim(),
                location.getText().toString().trim(), "Part-time", description.getText().toString().trim(), new String[0],
                salary.getText().toString().trim().isEmpty() ? null : salary.getText().toString().trim(), System.currentTimeMillis(), false));
            showTab(3);
        });
        look.add(body, post, 16);
        scroll.addView(body);
        page.addView(scroll, match());
        return page;
    }

    private View notePage(String title, String copy) {
        LinearLayout page = navPage(title);
        TextView text = look.text(copy, 17, 400, false, SaltLook.LABEL);
        text.setPadding(look.dp(16), look.dp(16), look.dp(16), look.dp(16));
        page.addView(text);
        return page;
    }

    private View formPage(String title, View form) {
        LinearLayout page = signedIn ? navPage(title) : plainNav(title);
        ScrollView scroll = scroll();
        form.setPadding(look.dp(24), look.dp(16), look.dp(24), look.dp(32));
        scroll.addView(form);
        page.addView(scroll, match());
        return page;
    }

    private LinearLayout buildTabBar() {
        LinearLayout bar = look.column();
        bar.setBackgroundColor(SaltLook.BACKGROUND);
        bar.addView(look.hairline());
        LinearLayout items = look.row(Gravity.CENTER);
        items.setPadding(0, look.dp(4), 0, look.dp(2));
        for (int i = 0; i < TAB_TITLES.length; i++) {
            int index = i;
            LinearLayout item = look.column();
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setPadding(0, look.dp(2), 0, look.dp(2));
            item.addView(look.icon(TAB_ICONS[i], SaltLook.SECONDARY_LABEL, 25));
            TextView label = look.text(TAB_TITLES[i], 10, 500, false, SaltLook.SECONDARY_LABEL);
            label.setGravity(Gravity.CENTER);
            look.add(item, label, 2);
            look.press(item, () -> showTab(index));
            items.addView(item, new LinearLayout.LayoutParams(0, look.dp(49), 1f));
        }
        bar.addView(items);
        bar.setVisibility(View.GONE);
        return bar;
    }

    private void paintTabs() {
        LinearLayout items = (LinearLayout) tabBar.getChildAt(1);
        for (int i = 0; i < items.getChildCount(); i++) {
            LinearLayout item = (LinearLayout) items.getChildAt(i);
            int color = i == tab ? SaltLook.PURPLE : SaltLook.SECONDARY_LABEL;
            ImageView icon = (ImageView) item.getChildAt(0);
            TextView label = (TextView) item.getChildAt(item.getChildCount() - 1);
            icon.setColorFilter(color);
            label.setTextColor(color);
        }
    }

    private View inlineBar() {
        LinearLayout bar = look.column();
        LinearLayout row = look.row(Gravity.CENTER);
        row.setMinimumHeight(look.dp(44));
        ImageView logo = new ImageView(activity);
        logo.setImageResource(R.drawable.salt_logo);
        row.addView(logo, new LinearLayout.LayoutParams(look.dp(24), look.dp(24)));
        TextView title = look.text("SALT", 17, 600, true, SaltLook.PURPLE);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.leftMargin = look.dp(6);
        row.addView(title, titleParams);
        bar.addView(row);
        bar.addView(look.hairline());
        return bar;
    }

    private LinearLayout navPage(String title) {
        LinearLayout page = look.column();
        page.addView(backBar(title));
        return page;
    }

    private LinearLayout plainNav(String title) {
        LinearLayout page = look.column();
        page.addView(backBar(title));
        return page;
    }

    private View backBar(String title) {
        LinearLayout bar = look.column();
        LinearLayout row = look.row(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(look.dp(44));
        row.setPadding(look.dp(8), 0, look.dp(16), 0);
        LinearLayout back = look.row(Gravity.CENTER_VERTICAL);
        back.addView(look.icon("ic_back", SaltLook.PURPLE, 22));
        TextView backLabel = look.text("Back", 17, 400, false, SaltLook.PURPLE);
        back.addView(backLabel);
        look.press(back, this::onBack);
        row.addView(back);
        TextView heading = look.text(title, 17, 600, true, SaltLook.LABEL);
        heading.setGravity(Gravity.CENTER);
        heading.setSingleLine(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.leftMargin = look.dp(8);
        row.addView(heading, params);
        View balance = new View(activity);
        row.addView(balance, new LinearLayout.LayoutParams(look.dp(64), 1));
        bar.addView(row);
        bar.addView(look.hairline());
        return bar;
    }

    private TextView largeTitle(String value) {
        TextView title = look.text(value, 34, 700, true, SaltLook.LABEL);
        title.setPadding(look.dp(16), look.dp(8), look.dp(16), look.dp(4));
        return title;
    }

    private View field(String title, EditText input) {
        LinearLayout box = look.column();
        box.addView(look.text(title, 13, 400, false, SaltLook.SECONDARY_LABEL));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = look.dp(6);
        box.addView(input, params);
        return box;
    }

    private View passwordRow(EditText password) {
        LinearLayout box = look.column();
        box.addView(look.text("Password", 13, 400, false, SaltLook.SECONDARY_LABEL));
        LinearLayout row = look.row(Gravity.CENTER_VERTICAL);
        row.setBackground(look.rounded(SaltLook.SECONDARY, 12));
        row.setPadding(0, 0, look.dp(12), 0);
        password.setBackground(null);
        password.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(password);
        ImageView eye = look.icon("ic_eye", SaltLook.SECONDARY_LABEL, 22);
        final boolean[] visible = {false};
        look.press(eye, () -> {
            visible[0] = !visible[0];
            int start = password.getSelectionStart();
            password.setTransformationMethod(visible[0] ? null : android.text.method.PasswordTransformationMethod.getInstance());
            eye.setImageResource(visible[0] ? R.drawable.ic_eye_off : R.drawable.ic_eye);
            password.setSelection(Math.max(0, start));
        });
        row.addView(eye);
        look.add(box, row, 6);
        return box;
    }

    private View orDivider() {
        LinearLayout row = look.row(Gravity.CENTER_VERTICAL);
        row.setPadding(0, look.dp(8), 0, look.dp(8));
        View left = new View(activity);
        left.setBackgroundColor(SaltLook.alpha(SaltLook.SECONDARY_LABEL, 0.3f));
        View right = new View(activity);
        right.setBackgroundColor(SaltLook.alpha(SaltLook.SECONDARY_LABEL, 0.3f));
        row.addView(left, new LinearLayout.LayoutParams(0, Math.max(1, look.dp(1)), 1f));
        TextView or = look.text("or", 12, 400, false, SaltLook.SECONDARY_LABEL);
        or.setPadding(look.dp(12), 0, look.dp(12), 0);
        row.addView(or);
        row.addView(right, new LinearLayout.LayoutParams(0, Math.max(1, look.dp(1)), 1f));
        return row;
    }

    private View searchWrap(EditText field) {
        FrameLayout frame = new FrameLayout(activity);
        field.setBackground(look.rounded(SaltLook.SEARCH, 10));
        field.setPadding(look.dp(36), look.dp(8), look.dp(12), look.dp(8));
        field.setInputType(InputType.TYPE_CLASS_TEXT);
        frame.addView(field, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        ImageView icon = look.icon("ic_search", SaltLook.SECONDARY_LABEL, 18);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(look.dp(18), look.dp(18));
        iconParams.gravity = Gravity.CENTER_VERTICAL;
        iconParams.leftMargin = look.dp(28);
        frame.addView(icon, iconParams);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.leftMargin = look.dp(16);
        params.rightMargin = look.dp(16);
        params.topMargin = look.dp(8);
        params.bottomMargin = look.dp(4);
        frame.setLayoutParams(params);
        return frame;
    }

    private EditText searchField(String hint) {
        return look.input(hint, false);
    }

    private HorizontalScrollView chips(String[] items, String selected, java.util.function.Consumer<String> pick) {
        HorizontalScrollView scroller = new HorizontalScrollView(activity);
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout row = look.row(Gravity.CENTER_VERTICAL);
        row.setPadding(look.dp(16), look.dp(12), look.dp(16), look.dp(12));
        for (String item : items) {
            boolean on = item.equals(selected);
            TextView chip = look.text(item, 12, 400, false, on ? android.graphics.Color.WHITE : SaltLook.LABEL);
            chip.setBackground(capsuleDrawable(on ? SaltLook.PURPLE : SaltLook.SECONDARY));
            chip.setPadding(look.dp(14), look.dp(8), look.dp(14), look.dp(8));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.rightMargin = look.dp(8);
            row.addView(chip, params);
            look.press(chip, () -> pick.accept(item));
        }
        scroller.addView(row);
        return scroller;
    }

    private FrameLayout avatar(String name, int size, float text) {
        FrameLayout frame = new FrameLayout(activity);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(SaltLook.alpha(SaltLook.PURPLE, size >= 72 ? 0.15f : 0.20f));
        frame.setBackground(circle);
        TextView initial = look.text(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.US), text, 600, true, SaltLook.PURPLE);
        initial.setGravity(Gravity.CENTER);
        frame.addView(initial, new FrameLayout.LayoutParams(look.dp(size), look.dp(size), Gravity.CENTER));
        frame.setLayoutParams(new LinearLayout.LayoutParams(look.dp(size), look.dp(size)));
        return frame;
    }

    private FrameLayout iconTile(String icon, int color, int size, float radius) {
        FrameLayout frame = new FrameLayout(activity);
        frame.setBackground(look.rounded(SaltLook.alpha(color, 0.12f), radius));
        ImageView image = look.icon(icon, color, size * 0.5f);
        frame.addView(image, new FrameLayout.LayoutParams(look.dp(size * 0.5f), look.dp(size * 0.5f), Gravity.CENTER));
        frame.setLayoutParams(new LinearLayout.LayoutParams(look.dp(size), look.dp(size)));
        return frame;
    }

    private TextView capsule(String value, float size, int foreground, int background, float padH, float padV) {
        TextView view = look.text(value, size, 700, false, foreground);
        view.setBackground(capsuleDrawable(background));
        view.setPadding(look.dp(padH), look.dp(padV), look.dp(padH), look.dp(padV));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.leftMargin = look.dp(6);
        view.setLayoutParams(params);
        return view;
    }

    private GradientDrawable capsuleDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(look.dp(100));
        return drawable;
    }

    private View cardBlock(String title, View content) {
        LinearLayout card = look.column();
        card.addView(look.text(title, 17, 600, true, SaltLook.LABEL));
        look.add(card, content, 8);
        return cardWrap(card);
    }

    private View cardWrap(View content) {
        content.setPadding(look.dp(16), look.dp(16), look.dp(16), look.dp(16));
        content.setBackground(look.rounded(SaltLook.SECONDARY, 16));
        if (content.getLayoutParams() == null) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = look.dp(16);
            content.setLayoutParams(params);
        }
        return content;
    }

    private View infoRow(String icon, String value, int color, Runnable tap) {
        LinearLayout row = look.row(Gravity.CENTER_VERTICAL);
        row.addView(look.icon(icon, color, 16));
        TextView text = look.text(value, 15, 400, false, color);
        text.setMaxLines(2);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.leftMargin = look.dp(8);
        row.addView(text, params);
        if (tap != null) look.press(row, tap);
        return row;
    }

    private View moreRow(String icon, String title, Runnable tap) {
        LinearLayout row = look.row(Gravity.CENTER_VERTICAL);
        row.setPadding(look.dp(16), look.dp(14), look.dp(16), look.dp(14));
        row.addView(look.icon(icon, SaltLook.PURPLE, 22));
        TextView label = look.text(title, 17, 400, false, SaltLook.LABEL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.leftMargin = look.dp(14);
        row.addView(label, params);
        row.addView(look.icon("ic_chevron", SaltLook.SECONDARY_LABEL, 18));
        look.press(row, tap);
        LinearLayout wrap = look.column();
        wrap.addView(row);
        wrap.addView(insetLine());
        return wrap;
    }

    private View settingRow(String title, String value, Runnable tap) {
        LinearLayout row = look.row(Gravity.CENTER_VERTICAL);
        row.setPadding(look.dp(16), look.dp(14), look.dp(16), look.dp(14));
        row.setBackgroundColor(android.graphics.Color.WHITE);
        row.addView(look.text(title, 17, 400, false, SaltLook.LABEL),
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (!value.isEmpty()) {
            TextView detail = look.text(value, 15, 400, false, value.startsWith("$") ? SaltLook.GOLD : SaltLook.SECONDARY_LABEL);
            row.addView(detail);
        }
        if (tap != null) row.addView(look.icon("ic_chevron", SaltLook.SECONDARY_LABEL, 18));
        look.press(row, tap == null ? () -> {} : tap);
        LinearLayout wrap = look.column();
        wrap.addView(row);
        wrap.addView(insetLine());
        return wrap;
    }

    private TextView sectionLabel(String value) {
        TextView label = look.text(value.toUpperCase(Locale.US), 13, 400, false, SaltLook.SECONDARY_LABEL);
        label.setPadding(look.dp(16), look.dp(18), look.dp(16), look.dp(6));
        label.setBackgroundColor(SaltLook.SECONDARY);
        return label;
    }

    private View empty(String title, String detail) {
        LinearLayout box = look.column();
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(look.dp(24), look.dp(48), look.dp(24), look.dp(24));
        TextView heading = look.text(title, 20, 600, true, SaltLook.LABEL);
        heading.setGravity(Gravity.CENTER);
        box.addView(heading);
        if (!detail.isEmpty()) {
            TextView copy = look.text(detail, 15, 400, false, SaltLook.SECONDARY_LABEL);
            copy.setGravity(Gravity.CENTER);
            look.add(box, copy, 8);
        }
        return box;
    }

    private View insetLine() {
        View line = new View(activity);
        line.setBackgroundColor(SaltLook.SEPARATOR);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, look.dp(0.5f)));
        params.leftMargin = look.dp(16);
        line.setLayoutParams(params);
        return line;
    }

    private View priceBlock(String title, String amount, int color) {
        LinearLayout box = look.column();
        box.addView(look.text(title, 12, 400, false, SaltLook.SECONDARY_LABEL));
        look.add(box, look.text(amount, 20, 600, true, color), 4);
        return box;
    }

    private ScrollView scroll() {
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SaltLook.BACKGROUND);
        return scroll;
    }

    private LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
    }

    private void clip(View view, float radius) {
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View outlined, Outline outline) {
                outline.setRoundRect(0, 0, outlined.getWidth(), outlined.getHeight(), look.dp(radius));
            }
        });
        view.setClipToOutline(true);
    }

    private void alert(String title, String message) {
        new AlertDialog.Builder(activity).setTitle(title).setMessage(message).setPositiveButton("OK", null).show();
    }

    private void browse(String url) {
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void discardPhoto() {
        if (pendingOriginal != null) Arrays.fill(pendingOriginal, (byte) 0);
        pendingOriginal = null;
    }

    private TextWatcher watcher(java.util.function.Consumer<String> consumer) {
        return new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) { consumer.accept(s.toString()); }
        };
    }

    private String money(Integer amount) {
        if (amount == null || amount == 0) return "Free";
        return "$" + amount;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.substring(0, 1).toUpperCase(Locale.US) + value.substring(1);
    }

    private String shortTime(long time) {
        DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US);
        format.setTimeZone(SaltData.ZONE);
        return format.format(new java.util.Date(time));
    }

    private boolean canVerify(String email, String phone, String password, String confirm) {
        int digits = 0;
        for (int i = 0; i < phone.length(); i++) if (Character.isDigit(phone.charAt(i))) digits++;
        return email.contains("@") && digits >= 10 && strong(password) && password.equals(confirm);
    }

    private boolean strong(String password) {
        boolean upper = false, lower = false, number = false, symbol = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) upper = true;
            else if (Character.isLowerCase(c)) lower = true;
            else if (Character.isDigit(c)) number = true;
            else symbol = true;
        }
        return password.length() >= 12 && upper && lower && number && symbol;
    }

    private String passwordRules(String password, String confirm) {
        return (password.length() >= 12 ? "● " : "○ ") + "At least 12 characters\n"
            + (hasUpper(password) && hasLower(password) ? "● " : "○ ") + "Uppercase and lowercase letters\n"
            + (hasDigit(password) && hasSymbol(password) ? "● " : "○ ") + "Number and symbol\n"
            + (!password.isEmpty() && password.equals(confirm) ? "● " : "○ ") + "Passwords match";
    }

    private boolean hasUpper(String value) { for (int i = 0; i < value.length(); i++) if (Character.isUpperCase(value.charAt(i))) return true; return false; }
    private boolean hasLower(String value) { for (int i = 0; i < value.length(); i++) if (Character.isLowerCase(value.charAt(i))) return true; return false; }
    private boolean hasDigit(String value) { for (int i = 0; i < value.length(); i++) if (Character.isDigit(value.charAt(i))) return true; return false; }
    private boolean hasSymbol(String value) { for (int i = 0; i < value.length(); i++) { char c = value.charAt(i); if (!Character.isLetterOrDigit(c)) return true; } return false; }

    private static final class GroupChat {
        String id = Long.toHexString(new SecureRandom().nextLong());
        String creatorId;
        final List<String> members = new ArrayList<>();
        String wrapped;
        byte[] salt;
        final KeyAttemptPolicy policy = new KeyAttemptPolicy();
        boolean blocked;
        boolean checking;
    }

    private static final class DirectChat {
        final byte[] salt = new byte[16];
        final List<String> messages = new ArrayList<>();
        final KeyAttemptPolicy policy = new KeyAttemptPolicy();
        String wrapped;
        byte[] sessionKey;
        boolean unlocked;
        boolean blocked;
        boolean checking;
        String notice;
    }
}
