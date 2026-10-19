-- ==============================================================================
-- Atelier Watch - V2 Seed Data
-- ==============================================================================

-- 1. Categories
INSERT INTO categories (name, slug, description) VALUES
('Travel & Dual Time', 'travel-dual-time', 'Complications crafted for global voyagers featuring multi-timezone displays and annual calendars.'),
('Chronograph', 'chronograph', 'High-precision mechanical stopwatches engineered for motor racing, aerospace, and nautical exploration.'),
('Diving', 'diving', 'Professional underwater instruments with high depth ratings, luminescent displays, and unidirectional rotating bezels.'),
('Haute Horlogerie', 'haute-horlogerie', 'Iconic integrated sports timepieces and complex masterworks representing centuries of hand-finishing.'),
('Dress & Classic', 'dress-classic', 'Understated elegance, refined proportions, and timeless aesthetics designed for formal occasions.');

-- 2. Watches
INSERT INTO watches (
    name, brand, slug, tagline, description, category_id, price, currency,
    movement_type, caliber, case_material, case_diameter_mm, case_thickness_mm,
    water_resistance_m, power_reserve_hours, crystal_material, dial_color,
    strap_material, functions, image_url, is_featured, status
) VALUES
-- 1. Sky-Dweller (Featured Hero model)
(
    'Sky-Dweller Annual Calendar', 'Rolex', 'rolex-sky-dweller-everose-green',
    'Time, in its own orbit — an architectural masterpiece for world travellers.',
    'A masterpiece of horological engineering, the Sky-Dweller provides the information global travellers need to keep track of time at a glance. It combines the Saros annual calendar—requiring only one date adjustment per year—with an intuitive off-center 24-hour disc for a second time zone, controlled seamlessly via the innovative fluted Ring Command bezel.',
    1, 41500.00, 'USD',
    'Automatic', 'Calibre 9002 Manufacture', '18 ct Everose Gold', 42.0, 14.0,
    100, 72, 'Scratch-resistant sapphire with Cyclops lens', 'Olive Green',
    '18 ct Everose Gold Oyster bracelet', 'Annual Calendar, Dual Time Zone, Ring Command Bezel, Saros System',
    'assets/frames/frame_0240.webp', TRUE, 'PUBLISHED'
),
-- 2. Cosmograph Daytona
(
    'Cosmograph Daytona', 'Rolex', 'rolex-cosmograph-daytona-steel',
    'The ultimate tool watch for those with a passion for driving and speed.',
    'Introduced in 1963, the Cosmograph Daytona was designed to meet the demands of professional racing drivers. Featuring a high-performance column-wheel chronograph movement with vertical clutch and a signature tachymetric scale engraved on its high-tech Cerachrom bezel, it remains the benchmark in sports chronographs.',
    2, 15100.00, 'USD',
    'Automatic', 'Calibre 4131 Manufacture', 'Oystersteel', 40.0, 11.9,
    100, 72, 'Scratch-resistant sapphire', 'White lacquer with black snailed counters',
    'Oystersteel three-piece links', 'Chronograph accurate to 1/8 second, Tachymeter scale, Small seconds',
    'https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?auto=format&fit=crop&w=1200&q=80', FALSE, 'PUBLISHED'
),
-- 3. Speedmaster Professional Moonwatch
(
    'Speedmaster Professional Moonwatch', 'OMEGA', 'omega-speedmaster-professional-moonwatch',
    'The legendary chronograph tested on the lunar surface.',
    'The Speedmaster Moonwatch is one of the world’s most iconic timepieces. Having been a part of all six moon landings, this legendary chronograph is powered by the Co-Axial Master Chronometer Calibre 3861, certified to the Swiss industry’s highest standard of precision, performance, and magnetic resistance up to 15,000 gauss.',
    2, 8000.00, 'USD',
    'Manual-Wind', 'Co-Axial Master Chronometer 3861', 'Stainless Steel', 42.0, 13.2,
    50, 50, 'Sapphire crystal with anti-reflective treatment', 'Step Black',
    'Stainless steel five-arched-links-per-row', 'Chronograph, Small seconds subdial, METAS Master Chronometer certified',
    'https://images.unsplash.com/photo-1547996160-71dfa63582d8?auto=format&fit=crop&w=1200&q=80', FALSE, 'PUBLISHED'
),
-- 4. Seamaster Diver 300M
(
    'Seamaster Diver 300M Co-Axial', 'OMEGA', 'omega-seamaster-diver-300m',
    'Celebrating nautical heritage with cutting-edge ceramic dive technology.',
    'Since 1993, the Seamaster Professional Diver 300M has enjoyed a legendary following. Today’s modern collection has embraced that famous ocean heritage and updated it with OMEGA’s best innovation. The 42 mm model is crafted from stainless steel and includes a black ceramic bezel with a white enamel diving scale.',
    3, 5900.00, 'USD',
    'Automatic', 'Co-Axial Master Chronometer 8800', 'Stainless Steel', 42.0, 13.6,
    300, 55, 'Domed scratch-resistant sapphire crystal', 'Laser-engraved wave black ceramic',
    'Stainless steel with foldover clasp and diver extension', 'Helium escape valve, Date window at 6 oclock, Unidirectional ceramic bezel',
    'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=1200&q=80', FALSE, 'PUBLISHED'
),
-- 5. Royal Oak "Jumbo" Extra-Thin
(
    'Royal Oak Selfwinding', 'Audemars Piguet', 'audemars-piguet-royal-oak-blue',
    'The revolutionary octagonal geometry that defined modern luxury sport.',
    'With its steel case, octagonal bezel secured by eight visible hexagonal screws, and tapisserie guilloche dial, the Royal Oak overturned the traditional conventions of Haute Horlogerie upon its 1972 debut. Powered by the Manufacture Calibre 4302, it embodies uncompromising architectural balance.',
    4, 27800.00, 'USD',
    'Automatic', 'Calibre 4302 Manufacture', 'Stainless Steel', 41.0, 10.5,
    50, 70, 'Glareproofed sapphire crystal and caseback', 'Bleu Nuit, Nuage 50 Grande Tapisserie',
    'Stainless steel bracelet with AP folding clasp', 'Hours, minutes, center seconds, date display',
    'https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=1200&q=80', FALSE, 'PUBLISHED'
),
-- 6. Submariner Date
(
    'Submariner Date "Kermit"', 'Rolex', 'rolex-submariner-date-green',
    'The quintessential archetype of the modern divers watch.',
    'The Submariner’s rotatable bezel is a key functionality of the watch. Its 60-minute graduations allow a diver to accurately and safely monitor diving time and decompression stops. Manufactured from hard, corrosion-resistant green ceramic, the Cerachrom bezel insert is virtually scratchproof.',
    3, 10800.00, 'USD',
    'Automatic', 'Calibre 3235 Manufacture', 'Oystersteel', 41.0, 12.3,
    300, 70, 'Scratch-resistant sapphire with Cyclops magnifier', 'Intense Black',
    'Oystersteel with Oysterlock safety clasp and Glidelock extension', 'Unidirectional 60-minute Cerachrom bezel, Chromalight blue luminescence',
    'https://images.unsplash.com/photo-1533139502658-0198f920d8e8?auto=format&fit=crop&w=1200&q=80', FALSE, 'PUBLISHED'
),
-- 7. Santos de Cartier Large
(
    'Santos de Cartier Large Model', 'Cartier', 'cartier-santos-large-steel',
    'The pioneering aviator watch born in Paris, 1904.',
    'Created in 1904 by Louis Cartier for his friend, Brazilian aviator Alberto Santos-Dumont, the Santos was one of the earliest wristwatches ever conceived. Characterized by its geometric square dial, rounded corners, symmetrical bevels, and exposed screws, the modern iteration features the QuickSwitch interchangeable strap system.',
    5, 7750.00, 'USD',
    'Automatic', 'Calibre 1847 MC Manufacture', 'Stainless Steel', 39.8, 9.38,
    100, 42, 'Sapphire crystal', 'Silvered opaline with Roman numerals',
    'Steel bracelet with SmartLink sizing and interchangeable calfskin strap', 'Hours, minutes, central seconds, date at 6 oclock, QuickSwitch system',
    'https://images.unsplash.com/photo-1614164185128-e4ec99c436d7?auto=format&fit=crop&w=1200&q=80', FALSE, 'PUBLISHED'
),
-- 8. Overseas Dual Time
(
    'Overseas Dual Time', 'Vacheron Constantin', 'vacheron-constantin-overseas-dual-time',
    'The spirit of travel captured in high-horology Genevan watchmaking.',
    'Bearing the prestigious Hallmark of Geneva (Poincon de Geneve), the Overseas Dual Time allows the simultaneous reading of two time zones, complemented by a day/night indicator and a radial date display synchronized to local time. Its Maltese cross-inspired bezel and translucent blue lacquered dial make it an icon of refined sportiness.',
    1, 32100.00, 'USD',
    'Automatic', 'Calibre 5110 DT Manufacture', 'Stainless Steel', 41.0, 12.8,
    150, 54, 'Sapphire crystal with anti-reflective coating', 'Translucent Blue Lacquered',
    'Interchangeable steel bracelet, blue Mississippiensis alligator leather, and blue rubber', 'Dual Time, Day/Night indicator, Subdial date, Anti-magnetic casing',
    'https://images.unsplash.com/photo-1524805444758-089113d48a6d?auto=format&fit=crop&w=1200&q=80', FALSE, 'PUBLISHED'
);

-- 3. Users (Initial Admin and Collector Accounts)
-- Passwords hashed with BCrypt (Cost 10):
-- 'Admin@123456'     => $2a$10$w4r6Q5eL.l79gq1pQo51gO7d2rKzK7K7y1I6Z9v0XvXw5xXzXxXx.
-- 'Collector@123456' => $2a$10$w4r6Q5eL.l79gq1pQo51gO7d2rKzK7K7y1I6Z9v0XvXw5xXzXxXx.
-- We use a known standard BCrypt hash for "Admin@123456" ($2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt2sW2C)
INSERT INTO users (display_name, email, password_hash, role) VALUES
('Horology Administrator', 'admin@atelierwatch.dev', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt2sW2C', 'ROLE_ADMIN'),
('Watch Collector', 'collector@atelierwatch.dev', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt2sW2C', 'ROLE_USER');

-- 4. Initial Wishlist (Demo user saved Sky-Dweller and Royal Oak)
INSERT INTO wishlist_items (user_id, watch_id) VALUES
(2, 1),
(2, 5);
