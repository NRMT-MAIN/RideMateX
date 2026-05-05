const API_BASE_URL = 'http://localhost:8081/api/v1';

// Tab switching
function switchTab(tabName) {
    const tabs = document.querySelectorAll('.tab-content');
    const buttons = document.querySelectorAll('.tab-button');

    tabs.forEach(tab => tab.classList.remove('active'));
    buttons.forEach(btn => btn.classList.remove('active'));

    document.getElementById(tabName).classList.add('active');
    event.target.classList.add('active');
}

// Alert helper
function showAlert(elementId, message, type) {
    const alert = document.getElementById(elementId);
    alert.textContent = message;
    alert.className = `alert ${type}`;
    console.log(`[${type.toUpperCase()}] ${message}`);
    setTimeout(() => {
        alert.style.display = 'none';
    }, 5000);
}

// ==================== PASSENGER FUNCTIONS ====================
async function registerPassenger() {
    const name = document.getElementById('passengerName').value;
    const email = document.getElementById('passengerEmail').value;
    const phoneNumber = document.getElementById('passengerPhone').value;
    const rating = parseFloat(document.getElementById('passengerRating').value);

    if (!name || !email || !phoneNumber || !rating) {
        showAlert('passengerAlert', 'Please fill all fields', 'error');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/passengers`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                name,
                email,
                phoneNumber
            })
        });

        if (response.ok) {
            const data = await response.json();
            showAlert('passengerAlert', `✓ Registered! Your ID: ${data.id}`, 'success');
            document.getElementById('bookingPassengerId').value = data.id;
            document.getElementById('passengerName').value = '';
            document.getElementById('passengerEmail').value = '';
            document.getElementById('passengerPhone').value = '';
            document.getElementById('passengerRating').value = '';
        } else {
            const errorData = await response.json();
            showAlert('passengerAlert', `Error: ${errorData.message || 'Registration failed'}`, 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('passengerAlert', `Error: ${error.message}`, 'error');
    }
}

async function bookRide() {
    const passengerId = document.getElementById('bookingPassengerId').value;
    const pickupLat = parseFloat(document.getElementById('pickupLat').value);
    const pickupLng = parseFloat(document.getElementById('pickupLng').value);
    const destLat = parseFloat(document.getElementById('destLat').value);
    const destLng = parseFloat(document.getElementById('destLng').value);
    const radius = parseFloat(document.getElementById('searchRadius').value);

    if (!passengerId || !pickupLat || !pickupLng || !destLat || !destLng) {
        showAlert('passengerAlert', 'Please fill all fields', 'error');
        return;
    }

    const nearbyDiv = document.getElementById('nearbyDrivers');
    nearbyDiv.innerHTML = '<div class="spinner"></div><p class="loading">Searching for nearby drivers...</p>';

    console.log('🔍 Searching for drivers near:', pickupLat, pickupLng, 'with radius:', radius);

    try {
        // Get nearby drivers
        const response = await fetch(`${API_BASE_URL}/location/nearbyDrivers`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                latitude: pickupLat,
                longitude: pickupLng,
                radius
            })
        });

        console.log('Response status:', response.status);

        if (response.ok) {
            const drivers = await response.json();
            console.log('📍 Found drivers:', drivers);

            if (drivers && drivers.length > 0) {
                displayNearbyDrivers(drivers, passengerId, pickupLat, pickupLng, destLat, destLng);
            } else {
                showAlert('passengerAlert', '❌ No nearby drivers found', 'error');
                nearbyDiv.innerHTML = '<p style="color: #666;">No nearby drivers available. Please try:</p><ul style="color: #666;"><li>Make sure drivers have updated their location</li><li>Increase the search radius</li><li>Try a different pickup location</li></ul>';
            }
        } else {
            const errorData = await response.json();
            showAlert('passengerAlert', `Error: ${errorData.message || 'Search failed'}`, 'error');
            nearbyDiv.innerHTML = '<p style="color: red;">Failed to search drivers</p>';
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('passengerAlert', `Error: ${error.message}`, 'error');
        nearbyDiv.innerHTML = '';
    }
}

function displayNearbyDrivers(drivers, passengerId, pickupLat, pickupLng, destLat, destLng) {
    let html = `<h3 style="margin-top: 30px; margin-bottom: 15px; color: #28a745;">✓ Found ${drivers.length} Nearby Driver(s)</h3>`;

    drivers.forEach(driver => {
        const distance = ((Math.sqrt(Math.pow(driver.latitude - pickupLat, 2) + Math.pow(driver.longitude - pickupLng, 2)) * 111000) / 1000).toFixed(2);
        html += `
            <div class="driver-card available">
                <div class="card-header">🚖 Driver ID: ${driver.driverId}</div>
                <div class="card-details">
                    <div class="card-detail-item">
                        <span class="card-detail-label">Location:</span>
                        <span>${driver.latitude.toFixed(4)}, ${driver.longitude.toFixed(4)}</span>
                    </div>
                    <div class="card-detail-item">
                        <span class="card-detail-label">Distance:</span>
                        <span><strong>${distance} km away</strong></span>
                    </div>
                </div>
                <button onclick="confirmBooking(${passengerId}, ${pickupLat}, ${pickupLng}, ${destLat}, ${destLng})">
                    📍 Request This Driver
                </button>
            </div>
        `;
    });

    document.getElementById('nearbyDrivers').innerHTML = html;
}

async function confirmBooking(passengerId, pickupLat, pickupLng, destLat, destLng) {
    try {
        console.log('📤 Creating booking...');

        const bookingData = {
            passengerId: parseInt(passengerId),
            pickupLocationLatitude: pickupLat,
            pickupLocationLongitude: pickupLng,
            dropoffLocation: `${destLat},${destLng}`,
            fare: 5.00
        };

        console.log('Booking payload:', bookingData);

        const response = await fetch(`${API_BASE_URL}/bookings`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(bookingData)
        });

        console.log('Response status:', response.status);

        if (response.ok) {
            const data = await response.json();
            showAlert('passengerAlert', `✓ Booking created! ID: ${data.id} - Notifying nearby drivers...`, 'success');
            document.getElementById('nearbyDrivers').innerHTML = '<p style="color: #28a745; font-weight: bold;">⏳ Waiting for a driver to accept...</p>';
        } else {
            const errorData = await response.json();
            console.error('Error response:', errorData);
            showAlert('passengerAlert', `Error: ${errorData.message || 'Booking failed'}`, 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('passengerAlert', `Error: ${error.message}`, 'error');
    }
}

// ==================== DRIVER FUNCTIONS ====================
async function registerDriver() {
    const name = document.getElementById('driverName').value;
    const email = document.getElementById('driverEmail').value;
    const phoneNumber = document.getElementById('driverPhone').value;
    const vehiclePlateNumber = document.getElementById('driverVehicle').value;
    const licenseNumber = document.getElementById('driverLicense').value;

    if (!name || !email || !phoneNumber || !vehiclePlateNumber || !licenseNumber) {
        showAlert('driverAlert', 'Please fill all fields', 'error');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/drivers`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                name,
                email,
                phoneNumber,
                vehiclePlateNumber,
                licenseNumber,
                isAvailable: true
            })
        });

        if (response.ok) {
            const data = await response.json();
            showAlert('driverAlert', `✓ Registered! Your ID: ${data.id}`, 'success');
            document.getElementById('driverIdLocation').value = data.id;
            document.getElementById('driverName').value = '';
            document.getElementById('driverEmail').value = '';
            document.getElementById('driverPhone').value = '';
            document.getElementById('driverVehicle').value = '';
            document.getElementById('driverLicense').value = '';
        } else {
            const errorData = await response.json();
            showAlert('driverAlert', `Error: ${errorData.message || 'Registration failed'}`, 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('driverAlert', `Error: ${error.message}`, 'error');
    }
}

async function updateDriverLocation() {
    const driverId = document.getElementById('driverIdLocation').value;
    const latitude = parseFloat(document.getElementById('driverLat').value);
    const longitude = parseFloat(document.getElementById('driverLng').value);

    if (!driverId || !latitude || !longitude) {
        showAlert('driverAlert', 'Please fill all fields', 'error');
        return;
    }

    console.log(`📍 Updating Driver ${driverId} location to:`, latitude, longitude);

    try {
        const response = await fetch(`${API_BASE_URL}/location/driverLocation`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                driverId: parseInt(driverId),
                latitude,
                longitude
            })
        });

        if (response.ok) {
            showAlert('driverAlert', '✓ Location updated! You are now visible to passengers', 'success');
            document.getElementById('driverLat').value = '';
            document.getElementById('driverLng').value = '';
        } else {
            const errorData = await response.json();
            showAlert('driverAlert', `Error: ${errorData.message || 'Failed to update location'}`, 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('driverAlert', `Error: ${error.message}`, 'error');
    }
}

async function loadAvailableDrivers() {
    const div = document.getElementById('availableDriversList');
    div.innerHTML = '<div class="spinner"></div><p class="loading">Loading drivers...</p>';

    console.log('📋 Loading available drivers...');

    try {
        const response = await fetch(`${API_BASE_URL}/drivers/available`);
        if (response.ok) {
            const drivers = await response.json();
            console.log('Available drivers:', drivers);
            displayAvailableDrivers(drivers);
        } else {
            div.innerHTML = '<p style="color: #666;">No drivers available</p>';
        }
    } catch (error) {
        console.error('Error:', error);
        div.innerHTML = `<p style="color: red;">Error: ${error.message}</p>`;
    }
}

function displayAvailableDrivers(drivers) {
    let html = '';
    if (!drivers || drivers.length === 0) {
        html = '<p style="color: #666;">No available drivers</p>';
    } else {
        drivers.forEach(driver => {
            html += `
                <div class="card">
                    <div class="card-header">🚗 ${driver.name}</div>
                    <div class="card-details">
                        <div class="card-detail-item">
                            <span class="card-detail-label">ID:</span>
                            <span>${driver.id}</span>
                        </div>
                        <div class="card-detail-item">
                            <span class="card-detail-label">Email:</span>
                            <span>${driver.email}</span>
                        </div>
                        <div class="card-detail-item">
                            <span class="card-detail-label">Phone:</span>
                            <span>${driver.phoneNumber}</span>
                        </div>
                        <div class="card-detail-item">
                            <span class="card-detail-label">Vehicle:</span>
                            <span>${driver.vehiclePlateNumber || 'N/A'}</span>
                        </div>
                        <div class="card-detail-item">
                            <span class="card-detail-label">License:</span>
                            <span>${driver.licenseNumber}</span>
                        </div>
                        <div class="card-detail-item">
                            <span class="card-detail-label">Status:</span>
                            <span>${driver.isAvailable ? '✓ Available' : '✗ Busy'}</span>
                        </div>
                    </div>
                </div>
            `;
        });
    }

    document.getElementById('availableDriversList').innerHTML = html;
}

// ==================== BOOKINGS FUNCTIONS ====================
async function loadPassengerBookings() {
    const passengerId = document.getElementById('passengerIdForBookings').value;
    if (!passengerId) {
        showAlert('bookingsAlert', 'Please enter passenger ID', 'error');
        return;
    }

    const div = document.getElementById('passengerBookingsList');
    div.innerHTML = '<div class="spinner"></div><p class="loading">Loading bookings...</p>';

    try {
        const response = await fetch(`${API_BASE_URL}/bookings/passenger/${passengerId}`);
        if (response.ok) {
            const bookings = await response.json();
            displayPassengerBookings(bookings);
        } else {
            div.innerHTML = '<p style="color: #666;">No bookings found</p>';
        }
    } catch (error) {
        console.error('Error:', error);
        div.innerHTML = `<p style="color: red;">Error: ${error.message}</p>`;
    }
}

function displayPassengerBookings(bookings) {
    let html = '';
    bookings.forEach(booking => {
        const statusValue = booking.status || 'PENDING';
        const statusClass = `status-${statusValue.toLowerCase()}`;
        html += `
            <div class="card">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
                    <div class="card-header">Booking #${booking.id}</div>
                    <span class="status-badge ${statusClass}">${statusValue}</span>
                </div>
                <div class="card-details">
                    <div class="card-detail-item">
                        <span class="card-detail-label">Driver ID:</span>
                        <span>${booking.driverId ? `#${booking.driverId}` : '⏳ Waiting...'}</span>
                    </div>
                    <div class="card-detail-item">
                        <span class="card-detail-label">Pickup:</span>
                        <span>${booking.pickupLocationLatitude}, ${booking.pickupLocationLongitude}</span>
                    </div>
                    <div class="card-detail-item">
                        <span class="card-detail-label">Destination:</span>
                        <span>${booking.dropoffLocation}</span>
                    </div>
                    <div class="card-detail-item">
                        <span class="card-detail-label">Fare:</span>
                        <span>$${booking.fare}</span>
                    </div>
                </div>
            </div>
        `;
    });

    document.getElementById('passengerBookingsList').innerHTML = html || '<p style="color: #666;">No bookings found</p>';
}

async function loadDriverBookings() {
    const driverId = document.getElementById('driverIdForBookings').value;
    if (!driverId) {
        showAlert('bookingsAlert', 'Please enter driver ID', 'error');
        return;
    }

    const div = document.getElementById('driverBookingsList');
    div.innerHTML = '<div class="spinner"></div><p class="loading">Loading bookings...</p>';

    console.log(`📋 Loading bookings for Driver ${driverId}...`);

    try {
        const response = await fetch(`${API_BASE_URL}/bookings/driver/${driverId}`);
        if (response.ok) {
            const bookings = await response.json();
            console.log('Driver bookings:', bookings);
            displayDriverBookings(bookings);
        } else {
            div.innerHTML = '<p style="color: #666;">No bookings found</p>';
        }
    } catch (error) {
        console.error('Error:', error);
        div.innerHTML = `<p style="color: red;">Error: ${error.message}</p>`;
    }
}

function displayDriverBookings(bookings) {
    let html = '';
    bookings.forEach(booking => {
        const statusValue = booking.status || 'PENDING';
        const statusClass = `status-${statusValue.toLowerCase()}`;
        html += `
            <div class="card">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
                    <div class="card-header">Booking #${booking.id}</div>
                    <span class="status-badge ${statusClass}">${statusValue}</span>
                </div>
                <div class="card-details">
                    <div class="card-detail-item">
                        <span class="card-detail-label">Passenger ID:</span>
                        <span>${booking.passengerId}</span>
                    </div>
                    <div class="card-detail-item">
                        <span class="card-detail-label">Pickup:</span>
                        <span>${booking.pickupLocationLatitude}, ${booking.pickupLocationLongitude}</span>
                    </div>
                    <div class="card-detail-item">
                        <span class="card-detail-label">Destination:</span>
                        <span>${booking.dropoffLocation}</span>
                    </div>
                    <div class="card-detail-item">
                        <span class="card-detail-label">Fare:</span>
                        <span>$${booking.fare}</span>
                    </div>
                </div>
                ${statusValue === 'PENDING' ? `
                    <button onclick="acceptBooking(${booking.id}, ${document.getElementById('driverIdForBookings').value})">✓ Accept Ride</button>
                ` : ''}
            </div>
        `;
    });

    document.getElementById('driverBookingsList').innerHTML = html || '<p style="color: #666;">No bookings found</p>';
}

async function acceptBooking(bookingId, driverId) {
    try {
        console.log(`✓ Driver ${driverId} accepting Booking ${bookingId}...`);

        const response = await fetch(`${API_BASE_URL}/bookings/${bookingId}/status?status=CONFIRMED`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' }
        });

        if (response.ok) {
            showAlert('bookingsAlert', '✓ Booking accepted! Driver marked as unavailable', 'success');
            setTimeout(() => {
                loadDriverBookings();
            }, 1000);
        } else {
            const errorData = await response.json();
            showAlert('bookingsAlert', `Error: ${errorData.message || 'Failed to accept booking'}`, 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showAlert('bookingsAlert', `Error: ${error.message}`, 'error');
    }
}
