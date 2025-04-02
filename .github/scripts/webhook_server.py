from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route('/webhook', methods=['POST'])
def webhook():
    # Try to parse the incoming JSON payload.
    data = request.get_json(silent=True)
    if data is None:
        return jsonify({'error': 'Invalid JSON'}), 400

    # Log or print the received payload.
    print("Received webhook payload:")
    print(data)

    # Optionally, you can inspect headers (e.g., for signatures or other metadata)
    auth_header = request.headers.get('Authorization')
    if auth_header:
        print("Authorization header:", auth_header)

    # Respond back to the sender.
    return jsonify({'status': 'success'}), 200

if __name__ == '__main__':
    # Run the server on port 5000 with debug enabled.
    app.run(host='0.0.0.0', port=5555, debug=True)
