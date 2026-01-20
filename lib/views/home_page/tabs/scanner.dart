import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:new_parking/views/details/parking_details_screen.dart';
import 'package:qr_code_scanner/qr_code_scanner.dart';

class ScannerScreen extends StatefulWidget {
  const ScannerScreen({Key? key}) : super(key: key);

  @override
  State<ScannerScreen> createState() => _ScannerScreenState();
}

class _ScannerScreenState extends State<ScannerScreen> {
  Barcode? result;
  QRViewController? controller;
  final GlobalKey qrKey = GlobalKey(debugLabel: 'QR');
  final TextEditingController _parkingIdController = TextEditingController();

  bool _navigating = false;

  @override
  void reassemble() {
    super.reassemble();
    // امنع crash لو controller لسه null
    if (controller == null) return;

    if (Platform.isAndroid) {
      controller!.pauseCamera();
    }
    controller!.resumeCamera();
  }

  @override
  void dispose() {
    _parkingIdController.dispose();
    controller?.dispose();
    super.dispose();
  }

  Widget _buildQrView(BuildContext context) {
    final scanArea = (MediaQuery.of(context).size.width < 400 ||
        MediaQuery.of(context).size.height < 400)
        ? 250.0
        : 320.0;

    return QRView(
      key: qrKey,
      onQRViewCreated: _onQRViewCreated,
      overlay: QrScannerOverlayShape(
        borderColor: Colors.cyan,
        borderRadius: 16,
        borderLength: 28,
        borderWidth: 8,
        cutOutSize: scanArea,
      ),
      onPermissionSet: (ctrl, p) => _onPermissionSet(context, ctrl, p),
    );
  }

  void _onQRViewCreated(QRViewController ctrl) {
    controller = ctrl;

    ctrl.scannedDataStream.listen((scanData) async {
      result = scanData;
      if (mounted) setState(() {});

      final code = (result?.code ?? '').trim();
      if (code.isEmpty) return;

      // امنع التكرار
      if (_navigating) return;
      _navigating = true;

      try {
        await controller?.pauseCamera();

        if (!mounted) return;
        await Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => ParkingDetailsScreen(parkingCode: code),
          ),
        );
      } finally {
        _navigating = false;
        await controller?.resumeCamera();
      }
    });
  }

  void _onPermissionSet(
      BuildContext context, QRViewController ctrl, bool granted) {
    if (!granted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Camera permission is required')),
      );
    }
  }

  Future<void> _openByManualCode() async {
    final code = _parkingIdController.text.trim();
    if (code.isEmpty) return;

    // امنع التكرار
    if (_navigating) return;
    _navigating = true;

    try {
      await controller?.pauseCamera();
      if (!mounted) return;

      await Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => ParkingDetailsScreen(parkingCode: code),
        ),
      );
    } finally {
      _navigating = false;
      await controller?.resumeCamera();
    }
  }

  ButtonStyle _cyanBtn() => ElevatedButton.styleFrom(
    backgroundColor: Colors.cyan,
    foregroundColor: Colors.white,
    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
  );

  @override
  Widget build(BuildContext context) {
    final codeText = (result?.code ?? '').trim();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Scanner'),
        backgroundColor: Colors.cyan,
        foregroundColor: Colors.white,
        systemOverlayStyle: SystemUiOverlayStyle.light,
      ),
      backgroundColor: Colors.grey.shade100,
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(12),
          children: [
            // Camera Card
            Card(
              elevation: 3,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(18),
              ),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(18),
                child: SizedBox(
                  height: MediaQuery.of(context).size.height * 0.34,
                  child: _buildQrView(context),
                ),
              ),
            ),

            const SizedBox(height: 12),

            // Result Box
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(14),
                boxShadow: const [
                  BoxShadow(
                    color: Colors.black12,
                    blurRadius: 6,
                    offset: Offset(0, 2),
                  )
                ],
              ),
              child: Row(
                children: [
                  const Icon(Icons.qr_code_2, color: Colors.cyan),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      codeText.isEmpty ? 'Scan Result will appear here…' : codeText,
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: codeText.isEmpty ? Colors.black45 : Colors.black87,
                      ),
                    ),
                  ),
                  if (codeText.isNotEmpty)
                    IconButton(
                      tooltip: 'Copy',
                      onPressed: () async {
                        await Clipboard.setData(ClipboardData(text: codeText));
                        if (!mounted) return;
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Copied')),
                        );
                      },
                      icon: const Icon(Icons.copy, color: Colors.cyan),
                    ),
                ],
              ),
            ),

            const SizedBox(height: 12),

            // Start / Stop
            Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    style: _cyanBtn(),
                    onPressed: () async => controller?.resumeCamera(),
                    icon: const Icon(Icons.play_arrow),
                    label: const Text('Start'),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.red,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(14),
                      ),
                    ),
                    onPressed: () async => controller?.pauseCamera(),
                    icon: const Icon(Icons.pause),
                    label: const Text('Stop'),
                  ),
                ),
              ],
            ),

            const SizedBox(height: 16),

            // Manual input
            Text(
              'Or enter parking number manually',
              style: TextStyle(
                color: Colors.grey.shade700,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 8),

            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _parkingIdController,
                    keyboardType: TextInputType.text,
                    textInputAction: TextInputAction.go,
                    onSubmitted: (_) => _openByManualCode(),
                    decoration: InputDecoration(
                      filled: true,
                      fillColor: Colors.white,
                      hintText: 'Parking number',
                      prefixIcon: const Icon(Icons.confirmation_number, color: Colors.cyan),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: const BorderSide(color: Colors.black12),
                      ),
                      enabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: const BorderSide(color: Colors.black12),
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: const BorderSide(color: Colors.cyan, width: 1.5),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                ElevatedButton(
                  style: _cyanBtn(),
                  onPressed: _openByManualCode,
                  child: const Text('Get'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
