-- ==============================================================================
-- Atelier Watch - V4 Multi-Brand E-Commerce Catalog Seed Data
-- ==============================================================================

-- Update existing watches with e-commerce references, stock, and provenance
UPDATE watches SET 
    reference_number = '326935-0005', 
    retail_price = 43500.00, 
    stock_quantity = 3, 
    warranty_years = 5,
    provenance_notes = 'Factory fresh, complete with original green presentation case, warranty card, and 5-year international guarantee.'
WHERE id = 1;

UPDATE watches SET 
    reference_number = '126500LN-0001', 
    retail_price = 15100.00, 
    stock_quantity = 2, 
    warranty_years = 5,
    provenance_notes = 'Accompanied by official chronometer certificate, manual, and guarantee valid worldwide.'
WHERE id = 2;

UPDATE watches SET 
    reference_number = '310.30.42.50.01.002', 
    retail_price = 8000.00, 
    stock_quantity = 6, 
    warranty_years = 5,
    provenance_notes = 'NASA flight-qualified pedigree, co-axial master chronometer certification card and moonwatch presentation box.'
WHERE id = 3;

UPDATE watches SET 
    reference_number = '210.30.42.20.01.001', 
    retail_price = 5900.00, 
    stock_quantity = 8, 
    warranty_years = 5,
    provenance_notes = 'METAS certified 15,000 gauss magnetic resistance with laser-engraved wave ceramic dial.'
WHERE id = 4;

UPDATE watches SET 
    reference_number = '15510ST.OO.1320ST.06', 
    retail_price = 28600.00, 
    stock_quantity = 2, 
    warranty_years = 5,
    provenance_notes = '50th anniversary caliber evolution, hand-finished satin-brushed octagonal case with white gold hexagonal screws.'
WHERE id = 5;

UPDATE watches SET 
    reference_number = '126610LV-0002', 
    retail_price = 10800.00, 
    stock_quantity = 4, 
    warranty_years = 5,
    provenance_notes = 'Unidirectional 60-minute Cerachrom green ceramic bezel with Chromalight long-lasting luminescence.'
WHERE id = 6;

UPDATE watches SET 
    reference_number = 'WSSA0018', 
    retail_price = 7750.00, 
    stock_quantity = 5, 
    warranty_years = 8,
    provenance_notes = 'Cartier Care extended 8-year warranty, QuickSwitch interchangeable strap system with additional calfskin strap.'
WHERE id = 7;

UPDATE watches SET 
    reference_number = '7900V/110A-B334', 
    retail_price = 32100.00, 
    stock_quantity = 2, 
    warranty_years = 5,
    provenance_notes = 'Hallmark of Geneva certified, accompanied by 3 interchangeable bracelets (steel, rubber, and alligator leather).'
WHERE id = 8;

-- Insert 4 new high-demand luxury brand models (Tudor, Grand Seiko, TAG Heuer, Breitling)
INSERT INTO watches (
    name, brand, slug, tagline, description, category_id, price, retail_price, currency,
    movement_type, caliber, case_material, case_diameter_mm, case_thickness_mm,
    water_resistance_m, power_reserve_hours, crystal_material, dial_color,
    strap_material, functions, image_url, is_featured, status,
    reference_number, stock_quantity, warranty_years, provenance_notes
) VALUES
-- 9. Tudor Black Bay 58
(
    'Black Bay 58 Navy Blue', 'Tudor', 'tudor-black-bay-58-navy',
    'A tribute to the brands historic divers watches with vintage proportions.',
    'The Black Bay 58 is named after the year 1958 in which the first Tudor 200m diver was introduced. Powered by the COSC-certified Manufacture Calibre MT5402, its slim 39mm case and matte navy blue dial embody understated vintage tool-watch excellence.',
    3, 3950.00, 4000.00, 'USD',
    'Automatic', 'Manufacture Calibre MT5402 (COSC)', 'Stainless Steel', 39.0, 11.9,
    200, 70, 'Domed scratch-resistant sapphire crystal', 'Matte Navy Blue',
    'Riveted steel bracelet with polished and satin finish', 'Hours, minutes, seconds, unidirectional rotatable bezel',
    'https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?auto=format&fit=crop&w=1200&q=80', FALSE, 'PUBLISHED',
    'M79030B-0001', 7, 5, 'COSC chronometer certified with transferable 5-year guarantee with no registration required.'
),
-- 10. Grand Seiko "Snowflake"
(
    'Heritage "Snowflake" Spring Drive', 'Grand Seiko', 'grand-seiko-snowflake-sbga211',
    'High-intensity titanium housing a dial inspired by the snow of the Shinshu mountains.',
    'The Snowflake SBGA211 is celebrated worldwide for its dial texture reminiscent of wind-blown snow on the mountains of Nagano. Driven by the revolutionary Spring Drive 9R65 caliber, it combines mechanical gear train durability with quartz electronic precision for a glide motion seconds hand.',
    5, 6200.00, 6200.00, 'USD',
    'Spring Drive', 'Calibre 9R65 Spring Drive', 'High-Intensity Titanium', 41.0, 12.5,
    100, 72, 'Dual-curved sapphire with anti-reflective coating', 'Textured Snowflake White',
    'High-intensity titanium bracelet with three-fold clasp', 'Spring Drive glide motion seconds, Date display, Power reserve indicator',
    'https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=1200&q=80', FALSE, 'PUBLISHED',
    'SBGA211', 4, 5, 'Zaratsu polished distortion-free titanium case, regulated to an extraordinary accuracy of ±1 second per day.'
),
-- 11. TAG Heuer Carrera Chronograph Glassbox
(
    'Carrera Chronograph "Glassbox"', 'TAG Heuer', 'tag-heuer-carrera-glassbox-blue',
    'Reinventing the iconic 1963 automotive dashboard racing chronograph.',
    'Commemorating 60 years of the Carrera, the Glassbox features a curved domed sapphire crystal that flows seamlessly over the tachymeter scale running around the dial rim. Powered by the in-house TH20-00 automatic chronograph movement with bi-directional winding.',
    2, 6450.00, 6450.00, 'USD',
    'Automatic', 'Calibre TH20-00 In-House', 'Fine-brushed Stainless Steel', 39.0, 13.86,
    100, 80, 'Beveled, domed Glassbox sapphire crystal', 'Signature Carrera Deep Blue',
    'Perforated blue calfskin leather strap with folding buckle', 'Chronograph: 1/4 second, 30 minutes counter, 12 hours counter, Date',
    'https://images.unsplash.com/photo-1547996160-71dfa63582d8?auto=format&fit=crop&w=1200&q=80', FALSE, 'PUBLISHED',
    'CBS2210.FC6534', 6, 5, 'Designed by Jack Heuer automotive heritage, backed by 5-year TAG Heuer warranty.'
),
-- 12. Breitling Navitimer B01 Chronograph 43
(
    'Navitimer B01 Chronograph 43', 'Breitling', 'breitling-navitimer-b01-ice-blue',
    'The undisputed icon of aviation navigation for over 70 years.',
    'First created in 1952 for the Aircraft Owners and Pilots Association (AOPA), the Navitimer remains the worlds favorite pilot watch. Featuring the circular slide rule for flight navigation calculations and the COSC-certified Breitling Manufacture Caliber 01 with a column wheel and vertical clutch.',
    2, 9100.00, 9300.00, 'USD',
    'Automatic', 'Breitling Caliber 01 (Manufacture)', 'Stainless Steel', 43.0, 13.6,
    30, 70, 'Cambered sapphire crystal, glareproofed both sides', 'Ice Blue with contrasting black counters',
    'Black alligator leather strap with stainless steel folding clasp', 'Circular slide rule, Chronograph 1/4th second, 30-minute and 12-hour totalizers',
    'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=1200&q=80', FALSE, 'PUBLISHED',
    'AB0138241C1A1', 3, 5, 'Equipped with Breitling Manufacture Caliber 01, certified Swiss chronometer with 5-year warranty.'
);
