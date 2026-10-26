package org.salt.savannah.ui;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** The same mock members, events, jobs, resources, and feed the iOS app shows. */
public final class SaltData {
    public static final TimeZone ZONE = TimeZone.getTimeZone("America/New_York");

    public static final class Member {
        public final String id;
        public final String email;
        public final String first;
        public final String last;
        public final String business;
        public final String tier;
        public final int dues;
        public final String status;
        public final long joined;
        public final String phone;
        public final String website;
        public final String bio;
        public final boolean board;
        public final String categories;

        Member(String id, String email, String first, String last, String business, String tier, int dues,
               String status, long joined, String phone, String website, String bio, boolean board, String categories) {
            this.id = id;
            this.email = email;
            this.first = first;
            this.last = last;
            this.business = business;
            this.tier = tier;
            this.dues = dues;
            this.status = status;
            this.joined = joined;
            this.phone = phone;
            this.website = website;
            this.bio = bio;
            this.board = board;
            this.categories = categories;
        }

        public String fullName() {
            return (first + " " + last).trim();
        }

        public String displayName() {
            return business == null || business.isEmpty() ? fullName() : business;
        }
    }

    public static final class Post {
        public final String id;
        public final String author;
        public final String business;
        public final String content;
        public final long created;
        public int likes;
        public int comments;
        public boolean liked;
        public final boolean official;
        public final boolean pinned;
        public byte[] image;

        Post(String id, String author, String business, String content, long created, int likes, int comments,
             boolean liked, boolean official, boolean pinned) {
            this.id = id;
            this.author = author;
            this.business = business;
            this.content = content;
            this.created = created;
            this.likes = likes;
            this.comments = comments;
            this.liked = liked;
            this.official = official;
            this.pinned = pinned;
        }
    }

    public static final class Comment {
        public final String postId;
        public final String author;
        public final String content;
        public final long created;

        Comment(String postId, String author, String content, long created) {
            this.postId = postId;
            this.author = author;
            this.content = content;
            this.created = created;
        }
    }

    public static final class Event {
        public final String title;
        public final String description;
        public final long start;
        public final String location;
        public final Integer memberPrice;
        public final Integer guestPrice;
        public final boolean signature;
        public final Integer max;
        public int attending;
        public final String category;
        public boolean rsvped;

        Event(String title, String description, long start, String location, Integer memberPrice, Integer guestPrice,
              boolean signature, Integer max, int attending, String category, boolean rsvped) {
            this.title = title;
            this.description = description;
            this.start = start;
            this.location = location;
            this.memberPrice = memberPrice;
            this.guestPrice = guestPrice;
            this.signature = signature;
            this.max = max;
            this.attending = attending;
            this.category = category;
            this.rsvped = rsvped;
        }
    }

    public static final class Job {
        public final String title;
        public final String company;
        public final String location;
        public final String type;
        public final String description;
        public final String[] requirements;
        public final String salary;
        public final long posted;
        public final boolean featured;

        Job(String title, String company, String location, String type, String description, String[] requirements,
            String salary, long posted, boolean featured) {
            this.title = title;
            this.company = company;
            this.location = location;
            this.type = type;
            this.description = description;
            this.requirements = requirements;
            this.salary = salary;
            this.posted = posted;
            this.featured = featured;
        }
    }

    public static final class Resource {
        public final String title;
        public final String description;
        public final String category;
        public final String url;
        public final String content;
        public final boolean membersOnly;
        public final String icon;

        Resource(String title, String description, String category, String url, String content, boolean membersOnly, String icon) {
            this.title = title;
            this.description = description;
            this.category = category;
            this.url = url;
            this.content = content;
            this.membersOnly = membersOnly;
            this.icon = icon;
        }
    }

    public static Member current = member("user-001", "demo@salt.savannah", "Jordan", "Ellis",
        "Historic Savannah Walking Tours", "For-Profit Business", 350, "active", monthsAgo(8),
        "(912) 555-0142", "https://historicsavannahtours.com",
        "Award-winning walking tours focused on the hidden stories of the Historic District.",
        true, "Tour Guide / Operator");

    public static final List<Member> members = new ArrayList<>(Arrays.asList(
        current,
        member("user-002", "maria@forsythparkcafe.com", "Maria", "Gonzalez", "Forsyth Park Café",
            "For-Profit Business", 350, "active", yearsAgo(1), "(912) 555-0198", null,
            "Farm-to-table café steps from Forsyth Park.", true, "Restaurant / Food"),
        member("user-003", "guide@savannahghosts.com", "Tyler", "Brooks", "Savannah Ghosts & Legends",
            "Individual / Sole Proprietor", 150, "active", monthsAgo(3), null, "https://savannahghosts.com",
            "Nighttime ghost tours with a historical twist.", false, "Tour Guide / Operator"),
        member("user-004", "info@riverstreetinn.com", "Patricia", "Hayes", "River Street Inn",
            "For-Profit Business", 350, "active", monthsAgo(14), "(912) 555-0177", "https://riverstreetinn.com",
            "Boutique historic inn on the riverfront.", false, "Hotel / Lodging"),
        member("user-005", "shop@southerncharm.com", "Lila", "Whitmore", "Southern Charm Boutique",
            "For-Profit Business", 350, "active", monthsAgo(5), "(912) 555-0133", null,
            "Curated Southern gifts and apparel on Broughton Street.", false, "Retail")
    ));

    public static final List<Post> posts = new ArrayList<>(Arrays.asList(
        new Post("post-001", "SALT Official", "Savannah Area Local Tours",
            "Welcome to the new SALT member app! Connect, network, and grow with fellow tourism businesses. Check the Events tab for our upcoming Monthly Mixer.",
            hoursAgo(3), 24, 5, false, true, true),
        new Post("post-002", "Maria Gonzalez", "Forsyth Park Café",
            "Looking for a reliable produce supplier who can deliver early mornings. Any recommendations from the group?",
            hoursAgo(8), 7, 4, true, false, false),
        new Post("post-003", "Tyler Brooks", "Savannah Ghosts & Legends",
            "New night tour route launching next month focusing on the Colonial Park Cemetery stories. Happy to collaborate on cross-promotion!",
            daysAgo(1), 15, 8, false, false, false)
    ));

    public static final List<Comment> comments = new ArrayList<>(Arrays.asList(
        new Comment("post-001", "Maria Gonzalez", "Excited to see everyone at the mixer.", hoursAgo(2)),
        new Comment("post-002", "Tyler Brooks", "I can send over the supplier we use.", hoursAgo(6)),
        new Comment("post-003", "Jordan Ellis", "Happy to coordinate a cross-promotion.", hoursAgo(12))
    ));

    public static final List<Event> events = new ArrayList<>(Arrays.asList(
        new Event("Monthly Member Networking Mixer",
            "Connect with fellow tourism businesses over drinks and conversation. Member pricing applies.",
            atHour(5, 18), "The Grey, 109 Martin Luther King Jr Blvd", 15, 35, true, 60, 28, "Networking", false),
        new Event("Business License & Permit Workshop",
            "Step-by-step guidance on applying for business licenses, tour operator permits, and city requirements.",
            atHour(12, 10), "SALT Office / Virtual Hybrid", 0, 25, false, 40, 19, "Education", true),
        new Event("Advocacy Roundtable: Parking & Right to Tour",
            "Open discussion with city officials on parking challenges and protecting the right to conduct tours in the Historic District.",
            atHour(19, 14), "City Hall Annex", 0, 0, true, null, 12, "Advocacy", false)
    ));

    public static final List<Job> jobs = new ArrayList<>(Arrays.asList(
        new Job("Experienced Walking Tour Guide", "Historic Savannah Walking Tours", "Historic District, Savannah",
            "Part-time", "Lead engaging walking tours focused on architecture and hidden history. Flexible schedule, weekends preferred.",
            new String[] {"Strong storytelling ability", "Knowledge of Savannah history", "Ability to walk 2–3 miles", "Customer service experience"},
            "$25–$35 / hour + tips", daysAgo(2), true),
        new Job("Server / Bartender", "Forsyth Park Café", "Forsyth Park area", "Part-time",
            "Join our team serving farm-to-table fare in a high-energy environment near the park.",
            new String[] {"Previous FOH experience", "Ability to work outdoors in season", "Team player"},
            "$12 / hour + tips", daysAgo(1), true),
        new Job("Front Desk Associate", "River Street Inn", "River Street, Savannah", "Full-time",
            "Greet guests, manage check-in/out, and provide concierge recommendations for local experiences.",
            new String[] {"Hospitality experience preferred", "Friendly professional demeanor", "Availability for evenings/weekends"},
            "$18–$22 / hour", daysAgo(5), false)
    ));

    public static final Resource[] resources = new Resource[] {
        new Resource("How to Apply for a Tour Operator Business License",
            "Step-by-step guide for obtaining or renewing your City of Savannah business license and tour operator permit.",
            "Licenses & Permits", "https://www.savannahga.gov/4035/Starting-a-Tour-Business",
            "Current City of Savannah guidance says all tour operators must register with the Special Events, Film, and Tourism Department before beginning operations.\n\n"
                + "New tour companies should contact the Tourism Division before submitting registration or applying for a Business Tax Certificate.\n\n"
                + "Registration materials include:\n• Company registration form\n• Business model or plan\n• Registration for all tour guides working for the company\n• Tour route map with starting and ending points\n• Operating times\n\n"
                + "Tour service vehicles, motorized or non-motorized, require annual inspection through Mobility & Parking Services.\n\n"
                + "After registration and any vehicle inspection steps, applicants obtain a Business Tax Certificate from the Revenue Department. Final approval is issued after Special Events, Film, and Tourism confirms the certificate through eProval.\n\n"
                + "Annual renewals are due no later than April 30 each calendar year.\n\nTourism Division: (912) 351-3837\nTourism@savannahga.gov\nMobility & Parking Services: (912) 651-6468",
            false, "ic_doc"),
        new Resource("SPD Crime Statistics & Safety Tips",
            "Latest Savannah Police Department reports relevant to the Historic District and tourism areas, plus practical safety recommendations.",
            "Crime & Safety", "https://savannahpd.org/crime-report/",
            "Savannah Police Department publishes weekly and annual crime report resources from its Crime Report page.\n\n"
                + "Useful contacts:\n• Information Desk: 201 Habersham Street, Savannah, GA 31401\n• Information Desk phone: 912-651-6675\n• Information Desk hours: 7:00 AM to 12:00 AM, Monday through Sunday\n• Northwest Precinct: 602 E. Lathrop Ave\n• Northwest Precinct phone: 912-651-6675\n\n"
                + "Crime tips:\n• SPD Crime Tip Line: 912-525-3124\n• CrimeStoppers: 912-234-2020\n• SPD Gang Unit: 912-651-3189",
            false, "ic_shield"),
        new Resource("Current Legislative Issues Affecting Tourism",
            "Updates on parking regulations, taxation proposals, workforce housing, and right-to-tour advocacy efforts.",
            "Legislative", "https://www.savannahga.gov/1479/Tourism-Advisory-Committee",
            "The City of Savannah Tourism Advisory Committee is a 10-member board representing tourism, resident, and preservation organizations.\n\n"
                + "The committee makes policy recommendations affecting:\n• Parking and routine tourism-related traffic activity\n• Enforcement of tourism management regulations\n• Tourism-related issues and regulations\n• Tourism trends\n• Savannah's tourism and hospitality industry\n• Quality of life and preservation concerns",
            true, "ic_columns"),
        new Resource("Insurance Requirements for Tour Operators",
            "Recommended coverage levels and local brokers familiar with tourism businesses.",
            "Business Support", "https://oci.georgia.gov/insurance-resources/business",
            "Georgia's Office of the Commissioner of Insurance and Safety Fire explains that business insurance helps protect against financial loss from property damage, theft, lawsuits, employee injuries, and illness.\n\n"
                + "Coverage types to review include:\n• Property insurance\n• Liability insurance\n• Workers' compensation\n• Motor vehicle insurance\n• Life and health insurance offered as employee benefits\n\n"
                + "Georgia law requires workers' compensation coverage for employers with three or more full- or part-time employees.",
            true, "ic_briefcase")
    };

    public static final String[] DIRECTORY = new String[] {
        "All", "Tour Guide / Operator", "Restaurant / Food", "Hotel / Lodging", "Retail", "Attraction", "Transportation", "Other Tourism"
    };
    public static final String[] EVENT_FILTERS = new String[] {"All", "Networking", "Education", "Social", "Advocacy", "Other"};
    public static final String[] RESOURCE_FILTERS = new String[] {"All", "Licenses & Permits", "Crime & Safety", "Legislative", "Business Support"};

    private SaltData() {}

    private static Member member(String id, String email, String first, String last, String business, String tier, int dues,
                                 String status, long joined, String phone, String website, String bio, boolean board, String categories) {
        return new Member(id, email, first, last, business, tier, dues, status, joined, phone, website, bio, board, categories);
    }

    public static String ago(long time) {
        long minutes = Math.max(1, (System.currentTimeMillis() - time) / 60000L);
        if (minutes < 60) return minutes + (minutes == 1 ? " min. ago" : " min. ago");
        long hours = minutes / 60;
        if (hours < 24) return hours + " hr. ago";
        long days = hours / 24;
        if (days == 1) return "1 day ago";
        return days + " days ago";
    }

    public static String daysLabel(long time) {
        long days = (System.currentTimeMillis() - time) / 86400000L;
        if (days <= 0) return "Today";
        if (days == 1) return "1 day ago";
        return days + " days ago";
    }

    public static String medium(long time) {
        DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US);
        format.setTimeZone(ZONE);
        return format.format(new Date(time));
    }

    public static String mediumTime(long time) {
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US);
        format.setTimeZone(ZONE);
        return format.format(new Date(time));
    }

    public static String month(long time) {
        Calendar calendar = Calendar.getInstance(ZONE);
        calendar.setTimeInMillis(time);
        return calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US);
    }

    public static String day(long time) {
        Calendar calendar = Calendar.getInstance(ZONE);
        calendar.setTimeInMillis(time);
        return String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
    }

    private static long hoursAgo(int hours) {
        return System.currentTimeMillis() - hours * 3600000L;
    }

    private static long daysAgo(int days) {
        return System.currentTimeMillis() - days * 86400000L;
    }

    private static long monthsAgo(int months) {
        Calendar calendar = Calendar.getInstance(ZONE);
        calendar.add(Calendar.MONTH, -months);
        return calendar.getTimeInMillis();
    }

    private static long yearsAgo(int years) {
        Calendar calendar = Calendar.getInstance(ZONE);
        calendar.add(Calendar.YEAR, -years);
        return calendar.getTimeInMillis();
    }

    private static long atHour(int daysAhead, int hour) {
        Calendar calendar = Calendar.getInstance(ZONE);
        calendar.add(Calendar.DAY_OF_YEAR, daysAhead);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
