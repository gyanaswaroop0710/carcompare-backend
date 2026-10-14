package com.carcompare;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Seeds sample cars on first run (only if the table is empty). Prices are approximate ex-showroom ₹ lakh. */
@Component
class DataSeeder implements CommandLineRunner {
    private final CarRepository repo;
    DataSeeder(CarRepository repo) { this.repo = repo; }

    @Override public void run(String... args) {
        if (repo.count() > 0) return;
        java.util.List<Car> cars = java.util.List.of(
                //        brand, model, year, body, fuel, trans, price, mileage, bhp, safety, seats, boot, features, color
                new Car("Maruti Suzuki","Alto K10",2024,"Hatchback","Petrol","Manual",4.2,24.4,66,2,5,214,"Touchscreen,Dual airbags,Power windows","#e8b04a"),
                new Car("Maruti Suzuki","Swift",2024,"Hatchback","Petrol","Manual",6.5,24.8,80,3,5,265,"Touchscreen,6 airbags,Auto AC","#d9534f"),
                new Car("Maruti Suzuki","Baleno",2024,"Hatchback","Petrol","Automatic",8.2,22.9,88,3,5,318,"360 camera,HUD,Wireless charging","#4a7fd9"),
                new Car("Maruti Suzuki","Brezza",2024,"SUV","CNG","Manual",9.1,25.5,87,4,5,328,"Sunroof,6 airbags,Cruise control","#7a8b99"),
                new Car("Maruti Suzuki","Ertiga",2024,"MPV","CNG","Manual",9.0,26.1,101,3,7,209,"3 rows,Touchscreen,Rear AC","#c9c9c9"),
                new Car("Tata","Tiago",2024,"Hatchback","Petrol","Manual",5.6,19.0,84,4,5,242,"4★ GNCAP,Harman audio,Projector lamps","#3fb5a0"),
                new Car("Tata","Punch",2024,"SUV","Petrol","Manual",6.2,20.1,87,5,5,366,"5★ GNCAP,High ground clearance,Touchscreen","#e57c3a"),
                new Car("Tata","Nexon",2024,"SUV","Diesel","Manual",9.8,23.2,113,5,5,382,"5★ GNCAP,Sunroof,Ventilated seats","#2f5d9e"),
                new Car("Tata","Nexon EV",2024,"SUV","Electric","Automatic",14.0,16.0,127,5,5,350,"5★ GNCAP,Fast charging,Connected tech","#1fa97a"),
                new Car("Tata","Harrier",2024,"SUV","Diesel","Automatic",15.5,16.8,167,5,5,445,"ADAS,Panoramic roof,JBL audio","#3b3b3b"),
                new Car("Hyundai","i20",2024,"Hatchback","Petrol","Automatic",8.4,20.3,87,3,5,311,"Bose audio,Sunroof,Wireless charging","#b04ad9"),
                new Car("Hyundai","Creta",2024,"SUV","Diesel","Automatic",13.5,21.0,114,3,5,433,"Panoramic sunroof,ADAS,Ventilated seats","#8a8f98"),
                new Car("Hyundai","Venue",2024,"SUV","Petrol","Manual",8.0,18.1,118,3,5,350,"Sunroof,Connected car,Turbo option","#d94a6a"),
                new Car("Hyundai","Verna",2024,"Sedan","Petrol","Automatic",11.0,18.6,158,5,5,528,"ADAS,Ventilated seats,Bose audio","#2d3a4a"),
                new Car("Honda","City",2024,"Sedan","Petrol","Automatic",12.0,17.8,119,5,5,506,"ADAS,Sunroof,LaneWatch","#c0c6cf"),
                new Car("Honda","Amaze",2024,"Sedan","Petrol","Manual",7.2,18.6,89,2,5,416,"Touchscreen,Rear camera,Auto AC","#9fb7d4"),
                new Car("Toyota","Innova Crysta",2024,"MPV","Diesel","Manual",19.9,15.6,148,4,7,300,"Captain seats,7 airbags,Rear AC","#6b5b4b"),
                new Car("Toyota","Fortuner",2024,"SUV","Diesel","Automatic",35.0,14.4,201,5,7,296,"4x4 option,Premium interior,Power tailgate","#f2f2f2"),
                new Car("Toyota","Urban Cruiser Hyryder",2024,"SUV","Hybrid","Automatic",12.0,27.9,92,3,5,373,"Strong hybrid,Panoramic roof,HUD","#4a9e6b"),
                new Car("Mahindra","Thar",2024,"SUV","Diesel","Manual",13.0,15.2,117,4,4,300,"4x4,Removable roof,Off-road capable","#a14a2f"),
                new Car("Mahindra","XUV700",2024,"SUV","Diesel","Automatic",16.0,16.5,182,5,7,240,"ADAS,Dual screens,Sony audio","#1f2a44"),
                new Car("Mahindra","Scorpio-N",2024,"SUV","Diesel","Manual",14.0,15.4,172,5,7,460,"4x4 option,Sunroof,Sony audio","#222222"),
                new Car("Kia","Seltos",2024,"SUV","Petrol","Automatic",12.5,17.0,158,3,5,433,"Ventilated seats,Bose audio,ADAS","#5b7bd9"),
                new Car("Kia","Carens",2024,"MPV","Diesel","Manual",11.5,21.2,114,3,7,216,"3 rows,Sunroof,Air purifier","#c76a8f"),
                new Car("Skoda","Slavia",2024,"Sedan","Petrol","Automatic",12.5,19.5,148,5,5,521,"5★ GNCAP,Ventilated seats,Sunroof","#b8c34a"),
                new Car("Volkswagen","Virtus",2024,"Sedan","Petrol","Automatic",12.0,19.4,148,5,5,521,"5★ GNCAP,Turbo petrol,Wireless carplay","#2f6fa8"),
                new Car("MG","Windsor EV",2024,"SUV","Electric","Automatic",14.0,18.0,134,4,5,604,"Big touchscreen,Reclining rear seats,Panoramic roof","#7ec8c0"),
                new Car("Tata","Tigor EV",2024,"Sedan","Electric","Automatic",12.5,17.0,74,4,5,316,"Connected tech,Fast charging,Harman audio","#46b4e0"),
                new Car("Renault","Kwid",2024,"Hatchback","Petrol","Manual",4.7,22.3,67,2,5,279,"Touchscreen,Rear camera,Digital cluster","#e0a24a"),
                // Luxury picks (approximate figures; verify before presenting)
                new Car("Audi","Q3",2024,"SUV","Petrol","Automatic",44.0,13.5,187,5,5,460,"Quattro AWD,Virtual cockpit,Panoramic sunroof","#8b95a5"),
                new Car("BMW","3 Series Gran Limousine",2024,"Sedan","Petrol","Automatic",62.0,14.0,255,5,5,480,"Curved display,Harman Kardon,Rear-wheel drive","#1c3d6e"),
                new Car("Mercedes-Benz","C-Class",2024,"Sedan","Petrol","Automatic",60.0,15.0,201,5,5,455,"MBUX,Burmester audio,Ambient lighting","#7d8794")
        );
        // model | torque Nm | engine cc (0 = electric) | transmission type | segment
        String[] specs = {
                "Alto K10|89|998|Manual|Entry Level",
                "Swift|112|1197|Manual|Entry Level",
                "Baleno|113|1197|AMT|Mid Range",
                "Brezza|122|1462|Manual|Mid Range",
                "Ertiga|122|1462|Manual|Family",
                "Tiago|113|1199|Manual|Entry Level",
                "Punch|115|1199|Manual|Entry Level",
                "Nexon|260|1497|Manual|Mid Range",
                "Nexon EV|215|0|Single-speed|Electric",
                "Harrier|350|1956|Torque Converter|Touring",
                "i20|115|1197|CVT|Mid Range",
                "Creta|250|1493|Torque Converter|Mid Range",
                "Venue|172|998|Manual|Mid Range",
                "Verna|253|1482|DCT|Touring",
                "City|145|1498|CVT|Touring",
                "Amaze|110|1199|Manual|Entry Level",
                "Innova Crysta|343|2393|Manual|Family",
                "Fortuner|500|2755|Torque Converter|Off-Road",
                "Urban Cruiser Hyryder|122|1490|e-CVT|Touring",
                "Thar|300|2184|Manual|Off-Road",
                "XUV700|450|2184|Torque Converter|Touring",
                "Scorpio-N|400|2184|Manual|Off-Road",
                "Seltos|253|1482|DCT|Mid Range",
                "Carens|250|1493|Manual|Family",
                "Slavia|250|1498|DCT|Touring",
                "Virtus|250|1498|DCT|Touring",
                "Windsor EV|200|0|Single-speed|Electric",
                "Tigor EV|170|0|Single-speed|Electric",
                "Kwid|91|999|Manual|Entry Level",
                "Q3|320|1984|DCT|Luxury",
                "3 Series Gran Limousine|400|1998|Torque Converter|Luxury",
                "C-Class|300|1496|Torque Converter|Luxury"
        };
        for (String s : specs) {
            String[] p = s.split("\\|");
            for (Car c : cars) if (c.getModel().equals(p[0])) c.setSpecs(Integer.parseInt(p[1]), Integer.parseInt(p[2]), p[3], p[4]);
        }
        repo.saveAll(cars);
    }
}