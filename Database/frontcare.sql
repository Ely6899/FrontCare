-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: bm1cdufjqwhe4cgtldeh-mysql.services.clever-cloud.com:3306
-- Generation Time: Feb 28, 2024 at 03:26 PM
-- Server version: 8.0.22-13
-- PHP Version: 8.2.16

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `bm1cdufjqwhe4cgtldeh`
--

-- --------------------------------------------------------

--
-- Table structure for table `donation_events`
--

CREATE TABLE `donation_events` (
  `event_id` int UNSIGNED NOT NULL,
  `donor_id` int UNSIGNED NOT NULL,
  `event_date` date NOT NULL,
  `event_location` varchar(45) NOT NULL,
  `event_address` varchar(45) NOT NULL,
  `remaining_spot` int UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `donation_events`
--

INSERT INTO `donation_events` (`event_id`, `donor_id`, `event_date`, `event_location`, `event_address`, `remaining_spot`) VALUES
(1, 2, '2024-02-01', 'Center', 'Tel Aviv', 99),
(2, 3, '2024-01-25', 'North', 'Haifa', 74),
(3, 4, '2024-04-27', 'Center', 'Kfar Saba', 98),
(4, 4, '2069-04-27', 'Center', 'Ariel', 88),
(5, 4, '2025-03-04', 'Center', 'Ariel', 100),
(6, 4, '2024-02-03', 'South', 'Eilat', 99),
(7, 4, '2025-02-02', 'Center', 'Hello', 776),
(8, 3, '2025-03-24', 'Center', 'tel aviv', 250),
(9, 3, '2025-02-08', 'South', 'Revava', 55),
(10, 3, '2024-02-03', 'South', 'test', 99),
(13, 3, '2025-06-20', 'South', 'Ely\'s Home', 7),
(14, 2, '2024-03-03', 'South', 'Millitary base', 100),
(15, 3, '2025-05-27', 'Center', 'Maor\'s House', 6),
(20, 3, '2024-02-27', 'Center', 'idan highway ', 1),
(26, 3, '2024-03-13', 'South', 'Revava', 200),
(31, 20, '2024-04-23', 'Center', 'Ariel', 1000);

-- --------------------------------------------------------

--
-- Table structure for table `event_details`
--

CREATE TABLE `event_details` (
  `event_id` int UNSIGNED NOT NULL,
  `product_id` int UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `event_details`
--

INSERT INTO `event_details` (`event_id`, `product_id`) VALUES
(1, 2),
(1, 3),
(1, 7),
(1, 6),
(2, 5),
(3, 0),
(3, 1),
(3, 2),
(3, 4),
(3, 6),
(3, 7),
(4, 1),
(4, 3),
(5, 0),
(5, 2),
(6, 1),
(6, 3),
(7, 2),
(7, 4),
(7, 6),
(8, 0),
(9, 2),
(9, 3),
(9, 4),
(10, 2),
(13, 1),
(13, 1),
(13, 4),
(13, 1),
(13, 2),
(13, 3),
(13, 4),
(14, 3),
(14, 4),
(15, 1),
(15, 3),
(15, 5),
(15, 1),
(15, 3),
(15, 5),
(20, 1),
(20, 2),
(20, 1),
(20, 2),
(20, 4),
(20, 1),
(20, 2),
(20, 4),
(26, 1),
(26, 2),
(26, 3),
(26, 4),
(31, 1),
(31, 2),
(31, 4),
(31, 5),
(31, 6);

-- --------------------------------------------------------

--
-- Table structure for table `event_participants`
--

CREATE TABLE `event_participants` (
  `event_id` int UNSIGNED NOT NULL,
  `user_id` int UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `event_participants`
--

INSERT INTO `event_participants` (`event_id`, `user_id`) VALUES
(1, 1),
(2, 1),
(1, 4),
(2, 4),
(3, 1),
(6, 16),
(10, 16),
(3, 4),
(20, 4),
(7, 22);

-- --------------------------------------------------------

--
-- Table structure for table `products`
--

CREATE TABLE `products` (
  `product_id` int UNSIGNED NOT NULL,
  `product_name` varchar(45) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `products`
--

INSERT INTO `products` (`product_id`, `product_name`) VALUES
(1, 'wipes'),
(2, 'clothes'),
(3, 'batteries'),
(4, 'vests'),
(5, 'headlight'),
(6, 'flashlight'),
(7, 'gloves'),
(8, 'charger');

-- --------------------------------------------------------

--
-- Table structure for table `request_details`
--

CREATE TABLE `request_details` (
  `request_id` int UNSIGNED NOT NULL,
  `product_id` int UNSIGNED NOT NULL,
  `quantity` int UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `request_details`
--

INSERT INTO `request_details` (`request_id`, `product_id`, `quantity`) VALUES
(1, 1, 3),
(1, 3, 1),
(2, 5, 10),
(3, 3, 2),
(3, 6, 1),
(4, 5, 100),
(16, 2, 50),
(18, 4, 10),
(18, 7, 10000),
(19, 1, 10000),
(19, 3, 55555),
(21, 1, 1000),
(21, 5, 55),
(16, 8, 10),
(23, 8, 150),
(23, 3, 150),
(24, 7, 50),
(24, 6, 100),
(25, 4, 5),
(25, 1, 8),
(27, 6, 100),
(27, 7, 55),
(28, 1, 3),
(4, 2, 50),
(29, 1, 2),
(29, 4, 3),
(29, 8, 2),
(29, 7, 2),
(29, 6, 2),
(29, 5, 2),
(29, 3, 2),
(29, 2, 8),
(30, 2, 1),
(30, 7, 22),
(33, 3, 250),
(33, 4, 250),
(33, 8, 250),
(34, 5, 5),
(33, 1, 555),
(39, 3, 100),
(39, 1, 100);

-- --------------------------------------------------------

--
-- Table structure for table `soldier_requests`
--

CREATE TABLE `soldier_requests` (
  `request_id` int UNSIGNED NOT NULL,
  `soldier_id` int UNSIGNED NOT NULL,
  `donor_id` int UNSIGNED DEFAULT NULL,
  `pickup_location` varchar(45) NOT NULL,
  `request_date` date NOT NULL,
  `close_date` date DEFAULT NULL,
  `status` varchar(45) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `soldier_requests`
--

INSERT INTO `soldier_requests` (`request_id`, `soldier_id`, `donor_id`, `pickup_location`, `request_date`, `close_date`, `status`) VALUES
(1, 1, 2, 'Center', '2022-12-01', '2024-01-28', 'closed'),
(2, 4, 3, 'North', '2022-12-01', '2024-12-01', 'closed'),
(3, 1, 4, 'Center', '2024-01-20', '2024-01-22', 'closed'),
(4, 4, 3, 'Center', '2024-02-03', NULL, 'pending'),
(16, 4, 3, 'Center', '2024-02-03', '2024-02-24', 'closed'),
(18, 1, 3, 'North', '2024-02-03', '2024-02-03', 'closed'),
(19, 16, 3, 'Center', '2024-02-03', '2024-02-03', 'closed'),
(21, 16, 2, 'South', '2024-02-03', '2024-02-03', 'closed'),
(23, 1, 2, 'North', '2024-02-05', '2024-02-14', 'closed'),
(24, 4, NULL, 'South', '2024-02-05', NULL, 'open'),
(25, 1, 2, 'Center', '2024-02-14', '2024-02-14', 'closed'),
(27, 1, 2, 'Center', '2024-02-14', '2024-02-17', 'closed'),
(28, 4, NULL, 'North', '2024-02-17', NULL, 'open'),
(29, 4, NULL, 'South', '2024-02-22', NULL, 'open'),
(30, 1, NULL, 'North', '2024-02-22', NULL, 'open'),
(33, 1, 20, 'Center', '2024-02-24', NULL, 'pending'),
(34, 17, 20, 'South', '2024-02-24', NULL, 'pending'),
(39, 22, NULL, 'Center', '2024-02-24', NULL, 'open');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `user_id` int UNSIGNED NOT NULL,
  `is_soldier` int UNSIGNED DEFAULT NULL,
  `firstname` varchar(45) NOT NULL,
  `lastname` varchar(45) NOT NULL,
  `username` varchar(45) NOT NULL,
  `password` varchar(45) NOT NULL,
  `location` varchar(45) DEFAULT NULL,
  `email_address` varchar(45) NOT NULL,
  `phone_number` varchar(45) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`user_id`, `is_soldier`, `firstname`, `lastname`, `username`, `password`, `location`, `email_address`, `phone_number`) VALUES
(1, 1, 'raz', 'saad', 'test', '098f6bcd4621d373cade4e832627b4f6', 'null', 'raz1@saad.com', '0501234568'),
(2, 0, 'ely', 'koz', 'ely6899', '487f3ba54ebe6bd564f333870902437d', 'Center', 'ely@koz.co.il', '0501234567'),
(3, 0, 'maor', 'or', 'maord', 'd47ff6ca32247954e3553f6fa1ef9f7a', 'South', 'maor@or.com', '0501234567'),
(4, 1, 'idan', 'high', 'idan', 'a9b52e6b3d8c1198fcb74fea21a6191a', 'null', 'idan@idan.com', '0501234567'),
(5, 1, 'test', 'test', 'bla', '128ecf542a35ac5270a87dc740918404', 'null', 'test', '0501234568'),
(6, 0, 'itizk', 'bitizk', 'itizk', '3d584f19e85c5dce3bf3e240d9db885c', 'South', 'itizk@bitizk.com', '0501234123'),
(7, 0, 'Ely', 'Kozinets', 'elikozinetz@gmail.com', '098f6bcd4621d373cade4e832627b4f6', 'Center', 'Ely', '0549728065'),
(8, 1, 'test2', 'test2', 'test3', '8ad8757baa8564dc136c1e07507f4a98', 'null', 'test2', '123'),
(16, 1, 'Maor', 'Or', 'maors', 'd47ff6ca32247954e3553f6fa1ef9f7a', 'null', 'GigaChad@gmail.com', '0525381648'),
(17, 1, 'Ely', 'Kozinets', 'Ely9986', '487f3ba54ebe6bd564f333870902437d', 'null', 'elikozinetz@gmail.com', '0549728065'),
(18, 0, 'Donor', 'Donor', 'niceDonor', '3d939a14c04ae16c98e3bddf6e8e4dd7', 'South', 'Donor@gmail.com', '0549721345'),
(19, 1, 'Ely', 'Kozinets', 'elys', '922e55e6e6e8e93e88f17106183300f3', 'null', 'elikozinetz@protonmail.com', '0549728065'),
(20, 0, 'Edmond', 'Rothschild', 'idand', 'a9b52e6b3d8c1198fcb74fea21a6191a', 'Center', 'rothschild@money.com', '0556666666'),
(21, 0, 'bla', 'bla', 'elt', '487f3ba54ebe6bd564f333870902437d', 'South', 'eli@gmail', '0059467901'),
(22, 1, 'Idan', 'Yanai', 'idans', 'a9b52e6b3d8c1198fcb74fea21a6191a', 'null', 'idan@atuda.e', '0558848084');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `donation_events`
--
ALTER TABLE `donation_events`
  ADD PRIMARY KEY (`event_id`);

--
-- Indexes for table `products`
--
ALTER TABLE `products`
  ADD PRIMARY KEY (`product_id`);

--
-- Indexes for table `soldier_requests`
--
ALTER TABLE `soldier_requests`
  ADD PRIMARY KEY (`request_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `username_UNIQUE` (`username`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `donation_events`
--
ALTER TABLE `donation_events`
  MODIFY `event_id` int UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=32;

--
-- AUTO_INCREMENT for table `products`
--
ALTER TABLE `products`
  MODIFY `product_id` int UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT for table `soldier_requests`
--
ALTER TABLE `soldier_requests`
  MODIFY `request_id` int UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=41;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `user_id` int UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=23;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
