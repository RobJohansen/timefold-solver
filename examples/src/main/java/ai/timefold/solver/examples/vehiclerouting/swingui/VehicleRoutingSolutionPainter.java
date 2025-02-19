package ai.timefold.solver.examples.vehiclerouting.swingui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.ImageIcon;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.examples.common.swingui.latitudelongitude.LatitudeLongitudeTranslator;
import ai.timefold.solver.examples.vehiclerouting.domain.Customer;
import ai.timefold.solver.examples.vehiclerouting.domain.Depot;
import ai.timefold.solver.examples.vehiclerouting.domain.Vehicle;
import ai.timefold.solver.examples.vehiclerouting.domain.VehicleRoutingSolution;
import ai.timefold.solver.examples.vehiclerouting.domain.location.AirLocation;
import ai.timefold.solver.examples.vehiclerouting.domain.location.DistanceType;
import ai.timefold.solver.examples.vehiclerouting.domain.location.Location;
import ai.timefold.solver.examples.vehiclerouting.domain.timewindowed.TimeWindowedCustomer;
import ai.timefold.solver.examples.vehiclerouting.domain.timewindowed.TimeWindowedDepot;
import ai.timefold.solver.examples.vehiclerouting.domain.timewindowed.TimeWindowedVehicleRoutingSolution;
import ai.timefold.solver.swing.impl.TangoColorFactory;

public class VehicleRoutingSolutionPainter {

    private static final int TEXT_SIZE = 12;
    private static final int TIME_WINDOW_DIAMETER = 26;
    private static final NumberFormat NUMBER_FORMAT = new DecimalFormat("#,##0.00");

    private static final String IMAGE_PATH_PREFIX = "/ai/timefold/solver/examples/vehiclerouting/swingui/";

    private ImageIcon depotImageIcon;
    private ImageIcon[] vehicleImageIcons;

    private BufferedImage canvas = null;
    private LatitudeLongitudeTranslator translator = null;
    private Long minimumTimeWindowTime = null;
    private Long maximumTimeWindowTime = null;

    public VehicleRoutingSolutionPainter() {
        depotImageIcon = new ImageIcon(getClass().getResource(IMAGE_PATH_PREFIX + "depot.png"));
        vehicleImageIcons = new ImageIcon[] {
                new ImageIcon(getClass().getResource(IMAGE_PATH_PREFIX + "vehicleChameleon.png")),
                new ImageIcon(getClass().getResource(IMAGE_PATH_PREFIX + "vehicleButter.png")),
                new ImageIcon(getClass().getResource(IMAGE_PATH_PREFIX + "vehicleSkyBlue.png")),
                new ImageIcon(getClass().getResource(IMAGE_PATH_PREFIX + "vehicleChocolate.png")),
                new ImageIcon(getClass().getResource(IMAGE_PATH_PREFIX + "vehiclePlum.png")),
        };
        int sequenceSize = TangoColorFactory.SEQUENCE_1.size();
        if (vehicleImageIcons.length != sequenceSize) {
            throw new IllegalStateException("The vehicleImageIcons length (" + vehicleImageIcons.length
                    + ") should be equal to the TangoColorFactory.SEQUENCE length (" + sequenceSize + ").");
        }
    }

    public BufferedImage getCanvas() {
        return canvas;
    }

    public LatitudeLongitudeTranslator getTranslator() {
        return translator;
    }

    public void reset(VehicleRoutingSolution solution, Dimension size, ImageObserver imageObserver) {
        translator = new LatitudeLongitudeTranslator();
        for (Location location : solution.getLocationList()) {
            translator.addCoordinates(location.getLatitude(), location.getLongitude());
        }
        determineMinimumAndMaximumTimeWindowTime(solution);

        double width = size.getWidth();
        double height = size.getHeight();
        translator.prepareFor(width, height - 10 - TEXT_SIZE);

        Graphics2D g = createCanvas(width, height);
        g.setFont(g.getFont().deriveFont((float) TEXT_SIZE));
        g.setStroke(TangoColorFactory.NORMAL_STROKE);
        for (Customer customer : solution.getCustomerList()) {
            Location location = customer.getLocation();
            int x = translator.translateLongitudeToX(location.getLongitude());
            int y = translator.translateLatitudeToY(location.getLatitude());
            g.setColor(TangoColorFactory.ALUMINIUM_4);
            g.fillRect(x - 1, y - 1, 3, 3);
            String demandString = Integer.toString(customer.getDemand());
            g.drawString(demandString, x - (g.getFontMetrics().stringWidth(demandString) / 2), y - TEXT_SIZE / 2);
            if (customer instanceof TimeWindowedCustomer timeWindowedCustomer) {
                g.setColor(TangoColorFactory.ALUMINIUM_3);
                int circleX = x - (TIME_WINDOW_DIAMETER / 2);
                int circleY = y + 5;
                g.drawOval(circleX, circleY, TIME_WINDOW_DIAMETER, TIME_WINDOW_DIAMETER);
                g.fillArc(circleX, circleY, TIME_WINDOW_DIAMETER, TIME_WINDOW_DIAMETER,
                        90 - calculateTimeWindowDegree(timeWindowedCustomer.getReadyTime()),
                        calculateTimeWindowDegree(timeWindowedCustomer.getReadyTime())
                                - calculateTimeWindowDegree(timeWindowedCustomer.getDueTime()));
                if (timeWindowedCustomer.getArrivalTime() != null) {
                    if (timeWindowedCustomer.isArrivalAfterDueTime()) {
                        g.setColor(TangoColorFactory.SCARLET_2);
                    } else if (timeWindowedCustomer.isArrivalBeforeReadyTime()) {
                        g.setColor(TangoColorFactory.ORANGE_2);
                    } else {
                        g.setColor(TangoColorFactory.ALUMINIUM_6);
                    }
                    g.setStroke(TangoColorFactory.THICK_STROKE);
                    int circleCenterY = y + 5 + TIME_WINDOW_DIAMETER / 2;
                    int angle = calculateTimeWindowDegree(timeWindowedCustomer.getArrivalTime());
                    g.drawLine(x, circleCenterY,
                            x + (int) (Math.sin(Math.toRadians(angle)) * (TIME_WINDOW_DIAMETER / 2 + 3)),
                            circleCenterY - (int) (Math.cos(Math.toRadians(angle)) * (TIME_WINDOW_DIAMETER / 2 + 3)));
                    g.setStroke(TangoColorFactory.NORMAL_STROKE);
                }
            }
        }
        g.setColor(TangoColorFactory.ALUMINIUM_3);
        for (Depot depot : solution.getDepotList()) {
            int x = translator.translateLongitudeToX(depot.getLocation().getLongitude());
            int y = translator.translateLatitudeToY(depot.getLocation().getLatitude());
            g.fillRect(x - 2, y - 2, 5, 5);
            g.drawImage(depotImageIcon.getImage(),
                    x - depotImageIcon.getIconWidth() / 2, y - 2 - depotImageIcon.getIconHeight(), imageObserver);
        }
        int colorIndex = 0;
        // TODO Too many nested for loops
        for (Vehicle vehicle : solution.getVehicleList()) {
            g.setColor(TangoColorFactory.SEQUENCE_2.get(colorIndex));
            // If there is only 1 customer in this chain, draw it on a line to the Depot anyway
            Location previousVehicleInfoLocation = vehicle.getLocation();
            Location vehicleInfoLocation = null;
            long longestNonDepotDistance = -1L;
            int load = 0;
            for (int i = 0; i < vehicle.getCustomers().size(); i++) {
                Customer customer = vehicle.getCustomers().get(i);
                load += customer.getDemand();
                Location previousLocation = i == 0
                        ? vehicle.getLocation()
                        : vehicle.getCustomers().get(i - 1).getLocation();
                Location location = customer.getLocation();
                translator.drawRoute(g, previousLocation.getLongitude(), previousLocation.getLatitude(),
                        location.getLongitude(), location.getLatitude(),
                        location instanceof AirLocation, false);
                // Determine where to draw the vehicle info
                long distance = previousLocation.getDistanceTo(location);
                if (longestNonDepotDistance < distance) {
                    longestNonDepotDistance = distance;
                    previousVehicleInfoLocation = previousLocation;
                    vehicleInfoLocation = location;
                }
                // Line back to the vehicle depot
                if (i == vehicle.getCustomers().size() - 1) {
                    Location vehicleLocation = vehicle.getLocation();
                    translator.drawRoute(g, location.getLongitude(), location.getLatitude(),
                            vehicleLocation.getLongitude(), vehicleLocation.getLatitude(),
                            location instanceof AirLocation, true);
                }
            }
            // Draw vehicle info
            if (vehicleInfoLocation != null) {
                if (load > vehicle.getCapacity()) {
                    g.setColor(TangoColorFactory.SCARLET_2);
                }
                double longitude = (previousVehicleInfoLocation.getLongitude() + vehicleInfoLocation.getLongitude()) / 2.0;
                int x = translator.translateLongitudeToX(longitude);
                double latitude = (previousVehicleInfoLocation.getLatitude() + vehicleInfoLocation.getLatitude()) / 2.0;
                int y = translator.translateLatitudeToY(latitude);
                boolean ascending = (previousVehicleInfoLocation.getLongitude() < vehicleInfoLocation.getLongitude())
                        ^ (previousVehicleInfoLocation.getLatitude() < vehicleInfoLocation.getLatitude());

                ImageIcon vehicleImageIcon = vehicleImageIcons[colorIndex];
                int vehicleInfoHeight = vehicleImageIcon.getIconHeight() + 2 + TEXT_SIZE;
                g.drawImage(vehicleImageIcon.getImage(),
                        x + 1, (ascending ? y - vehicleInfoHeight - 1 : y + 1), imageObserver);
                g.drawString(load + " / " + vehicle.getCapacity(),
                        x + 1, (ascending ? y - 1 : y + vehicleInfoHeight + 1));
            }
            colorIndex = (colorIndex + 1) % TangoColorFactory.SEQUENCE_2.size();
        }

        // Legend
        g.setColor(TangoColorFactory.ALUMINIUM_3);
        g.fillRect(5, (int) height - 12 - TEXT_SIZE - (TEXT_SIZE / 2), 5, 5);
        g.drawString("Depot", 15, (int) height - 10 - TEXT_SIZE);
        String vehiclesSizeString = solution.getVehicleList().size() + " vehicles";
        g.drawString(vehiclesSizeString,
                ((int) width - g.getFontMetrics().stringWidth(vehiclesSizeString)) / 2, (int) height - 10 - TEXT_SIZE);
        g.setColor(TangoColorFactory.ALUMINIUM_4);
        g.fillRect(6, (int) height - 6 - (TEXT_SIZE / 2), 3, 3);
        g.drawString((solution instanceof TimeWindowedVehicleRoutingSolution)
                ? "Customer: demand, time window and arrival time"
                : "Customer: demand", 15, (int) height - 5);
        String customersSizeString = solution.getCustomerList().size() + " customers";
        g.drawString(customersSizeString,
                ((int) width - g.getFontMetrics().stringWidth(customersSizeString)) / 2, (int) height - 5);
        if (solution.getDistanceType() == DistanceType.AIR_DISTANCE) {
            String clickString = "Right click anywhere on the map to add a customer.";
            g.drawString(clickString, (int) width - 5 - g.getFontMetrics().stringWidth(clickString), (int) height - 5);
        }
        // Show soft score
        g.setColor(TangoColorFactory.ORANGE_3);
        HardSoftLongScore score = solution.getScore();
        if (score != null) {
            String distanceString;
            if (!score.isFeasible()) {
                distanceString = "Not feasible";
            } else {
                distanceString = solution.getDistanceString(NUMBER_FORMAT);
            }
            g.setFont(g.getFont().deriveFont(Font.BOLD, (float) TEXT_SIZE * 2));
            g.drawString(distanceString,
                    (int) width - g.getFontMetrics().stringWidth(distanceString) - 10, (int) height - 10 - TEXT_SIZE);
        }
    }

    private void determineMinimumAndMaximumTimeWindowTime(VehicleRoutingSolution solution) {
        minimumTimeWindowTime = Long.MAX_VALUE;
        maximumTimeWindowTime = Long.MIN_VALUE;
        for (Depot depot : solution.getDepotList()) {
            if (depot instanceof TimeWindowedDepot timeWindowedDepot) {
                long readyTime = timeWindowedDepot.getReadyTime();
                if (readyTime < minimumTimeWindowTime) {
                    minimumTimeWindowTime = readyTime;
                }
                long dueTime = timeWindowedDepot.getDueTime();
                if (dueTime > maximumTimeWindowTime) {
                    maximumTimeWindowTime = dueTime;
                }
            }
        }
        for (Customer customer : solution.getCustomerList()) {
            if (customer instanceof TimeWindowedCustomer timeWindowedCustomer) {
                long readyTime = timeWindowedCustomer.getReadyTime();
                if (readyTime < minimumTimeWindowTime) {
                    minimumTimeWindowTime = readyTime;
                }
                long dueTime = timeWindowedCustomer.getDueTime();
                if (dueTime > maximumTimeWindowTime) {
                    maximumTimeWindowTime = dueTime;
                }
            }
        }
    }

    private int calculateTimeWindowDegree(long timeWindowTime) {
        return (int) (360L * (timeWindowTime - minimumTimeWindowTime) / (maximumTimeWindowTime - minimumTimeWindowTime));
    }

    public Graphics2D createCanvas(double width, double height) {
        int canvasWidth = (int) Math.ceil(width) + 1;
        int canvasHeight = (int) Math.ceil(height) + 1;
        canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, canvasWidth, canvasHeight);
        return g;
    }

}
